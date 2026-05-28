"""Инциденты: создание очевидцем (фото/видео/гео), просмотр и обработка инспектором."""
import shutil
import uuid
from pathlib import Path
from typing import List, Optional

from fastapi import (
    APIRouter, Depends, HTTPException, status, UploadFile, File, Form, Request
)
from sqlalchemy.orm import Session

from app.core.config import MEDIA_DIR, MAX_UPLOAD_SIZE
from app.core.database import get_db
from app.core.deps import get_current_user, require_roles
from app.core.push import send_push
from app.models.models import User, Role, Incident, MediaFile, IncidentStatus, Patrol
from app.schemas.schemas import IncidentOut, MediaOut

router = APIRouter(prefix="/incidents", tags=["incidents"])


def _build_incident_out(incident: Incident, request: Request) -> IncidentOut:
    """Собирает ответ, добавляя абсолютные URL к медиафайлам."""
    base = str(request.base_url).rstrip("/")
    media = [
        MediaOut(
            id=m.id,
            filename=m.filename,
            media_type=m.media_type,
            url=f"{base}/media/{m.filename}",
        )
        for m in incident.media
    ]
    out = IncidentOut.model_validate(incident)
    out.media = media
    return out


@router.post("", response_model=IncidentOut)
async def create_incident(
    request: Request,
    description: Optional[str] = Form(None),
    latitude: Optional[float] = Form(None),
    longitude: Optional[float] = Form(None),
    files: List[UploadFile] = File(default=[]),
    current: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Очевидец создаёт инцидент. Можно приложить несколько фото/видео и геолокацию."""
    incident = Incident(
        author_id=current.id,
        description=description,
        latitude=latitude,
        longitude=longitude,
        status=IncidentStatus.new,
    )
    db.add(incident)
    db.flush()  # получаем incident.id до сохранения файлов

    for upload in files:
        if not upload.filename:
            continue
        ext = Path(upload.filename).suffix
        stored_name = f"{uuid.uuid4().hex}{ext}"
        dest = MEDIA_DIR / stored_name

        # Контролируем размер при потоковой записи
        size = 0
        with open(dest, "wb") as buf:
            while chunk := await upload.read(1024 * 1024):
                size += len(chunk)
                if size > MAX_UPLOAD_SIZE:
                    buf.close()
                    dest.unlink(missing_ok=True)
                    raise HTTPException(
                        status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
                        detail="Файл слишком большой",
                    )
                buf.write(chunk)

        content_type = (upload.content_type or "")
        media_type = "video" if content_type.startswith("video") else "photo"
        db.add(MediaFile(
            incident_id=incident.id,
            filename=stored_name,
            original_name=upload.filename,
            media_type=media_type,
        ))

    db.commit()
    db.refresh(incident)

    # Уведомляем инспекторов, которые сейчас на патруле и не отключили уведомления
    _notify_patrolling_inspectors(db, incident)

    return _build_incident_out(incident, request)


def _notify_patrolling_inspectors(db: Session, incident: Incident):
    """Рассылает пуш всем инспекторам с активным патрулём и включёнными уведомлениями."""
    active_inspector_ids = (
        db.query(Patrol.inspector_id)
        .filter(Patrol.ended_at.is_(None))
        .distinct()
        .all()
    )
    ids = [row[0] for row in active_inspector_ids]
    if not ids:
        return
    recipients = (
        db.query(User)
        .filter(
            User.id.in_(ids),
            User.is_active.is_(True),
            User.notifications_enabled.is_(True),
            User.fcm_token.isnot(None),
        )
        .all()
    )
    title = "Новое сообщение от очевидца"
    body = incident.description or "Поступило сообщение о происшествии"
    for user in recipients:
        send_push(
            user.fcm_token,
            title,
            body[:120],
            data={"incident_id": incident.id, "type": "new_incident"},
        )
def my_incidents(
    request: Request,
    current: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Очевидец смотрит свои отправленные инциденты."""
    items = (
        db.query(Incident)
        .filter(Incident.author_id == current.id)
        .order_by(Incident.created_at.desc())
        .all()
    )
    return [_build_incident_out(i, request) for i in items]


@router.get("", response_model=List[IncidentOut])
def list_incidents(
    request: Request,
    only_new: bool = False,
    current: User = Depends(require_roles(Role.inspector, Role.admin, Role.chief)),
    db: Session = Depends(get_db),
):
    """Лента инцидентов для сотрудников. only_new=True — только необработанные."""
    q = db.query(Incident)
    if only_new:
        q = q.filter(Incident.status == IncidentStatus.new)
    items = q.order_by(Incident.created_at.desc()).all()
    return [_build_incident_out(i, request) for i in items]


@router.post("/{incident_id}/accept", response_model=IncidentOut)
def accept_incident(
    incident_id: int,
    request: Request,
    current: User = Depends(require_roles(Role.inspector, Role.admin, Role.chief)),
    db: Session = Depends(get_db),
):
    """Инспектор принимает инцидент в работу. Требуется активный патруль."""
    active_patrol = (
        db.query(Patrol)
        .filter(Patrol.inspector_id == current.id, Patrol.ended_at.is_(None))
        .first()
    )
    if active_patrol is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Нельзя принять инцидент вне патруля. Начните патруль.",
        )
    incident = db.query(Incident).filter(Incident.id == incident_id).first()
    if incident is None:
        raise HTTPException(status_code=404, detail="Инцидент не найден")
    incident.status = IncidentStatus.accepted
    incident.accepted_by_id = current.id
    db.commit()
    db.refresh(incident)
    return _build_incident_out(incident, request)


@router.post("/{incident_id}/close", response_model=IncidentOut)
def close_incident(
    incident_id: int,
    request: Request,
    current: User = Depends(require_roles(Role.inspector, Role.admin, Role.chief)),
    db: Session = Depends(get_db),
):
    """Закрыть инцидент (обработан)."""
    incident = db.query(Incident).filter(Incident.id == incident_id).first()
    if incident is None:
        raise HTTPException(status_code=404, detail="Инцидент не найден")
    incident.status = IncidentStatus.closed
    if incident.accepted_by_id is None:
        incident.accepted_by_id = current.id
    db.commit()
    db.refresh(incident)
    return _build_incident_out(incident, request)
