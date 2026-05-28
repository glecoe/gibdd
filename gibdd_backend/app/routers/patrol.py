"""Патрули: инспектор начинает и заканчивает смену.

Пока есть незакрытый патруль (ended_at IS NULL) — инспектор считается 'на смене'
и ему показываются/доставляются новые инциденты.
"""
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.deps import require_roles
from app.models.models import User, Role, Patrol
from app.models.models import utcnow
from app.schemas.schemas import PatrolOut

router = APIRouter(prefix="/patrol", tags=["patrol"])

_INSPECTOR_ROLES = (Role.inspector, Role.admin, Role.chief)


def _active_patrol(db: Session, user_id: int) -> Optional[Patrol]:
    return (
        db.query(Patrol)
        .filter(Patrol.inspector_id == user_id, Patrol.ended_at.is_(None))
        .first()
    )


@router.post("/start", response_model=PatrolOut)
def start_patrol(
    current: User = Depends(require_roles(*_INSPECTOR_ROLES)),
    db: Session = Depends(get_db),
):
    """Начать патруль. Если уже есть активный — возвращаем его (идемпотентно)."""
    existing = _active_patrol(db, current.id)
    if existing:
        return existing
    patrol = Patrol(inspector_id=current.id)
    db.add(patrol)
    db.commit()
    db.refresh(patrol)
    return patrol


@router.post("/stop", response_model=PatrolOut)
def stop_patrol(
    current: User = Depends(require_roles(*_INSPECTOR_ROLES)),
    db: Session = Depends(get_db),
):
    """Закончить патруль."""
    patrol = _active_patrol(db, current.id)
    if patrol is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Активный патруль не найден",
        )
    patrol.ended_at = utcnow()
    db.commit()
    db.refresh(patrol)
    return patrol


@router.get("/status")
def patrol_status(
    current: User = Depends(require_roles(*_INSPECTOR_ROLES)),
    db: Session = Depends(get_db),
):
    """Текущий статус: на патруле или нет."""
    patrol = _active_patrol(db, current.id)
    return {
        "on_patrol": patrol is not None,
        "patrol_id": patrol.id if patrol else None,
        "started_at": patrol.started_at if patrol else None,
    }
