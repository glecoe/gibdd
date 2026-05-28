"""Администрирование.

Права:
  admin  — видит пользователей, создаёт инспекторов, назначает роли (кроме admin/chief),
           управляет СВОИМИ уведомлениями.
  chief  — всё, что admin, плюс назначает/удаляет администраторов и деактивирует пользователей.
"""
from typing import List

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.security import hash_password
from app.core.deps import get_current_user, require_roles
from app.models.models import User, Role
from app.schemas.schemas import UserOut, UserCreate, RoleUpdate, NotificationsUpdate

router = APIRouter(prefix="/admin", tags=["admin"])


@router.get("/users", response_model=List[UserOut])
def list_users(
    current: User = Depends(require_roles(Role.admin, Role.chief)),
    db: Session = Depends(get_db),
):
    return db.query(User).order_by(User.created_at.desc()).all()


@router.post("/users", response_model=UserOut)
def create_user(
    data: UserCreate,
    current: User = Depends(require_roles(Role.admin, Role.chief)),
    db: Session = Depends(get_db),
):
    """Создание сотрудника. Админ не может создавать admin/chief — только начальник."""
    if data.role in (Role.admin, Role.chief) and current.role != Role.chief:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Только начальник может назначать администраторов и начальников",
        )
    if db.query(User).filter(User.phone == data.phone).first():
        raise HTTPException(status_code=400, detail="Пользователь с таким телефоном уже есть")

    user = User(
        phone=data.phone,
        password_hash=hash_password(data.password),
        full_name=data.full_name,
        role=data.role,
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return user


@router.patch("/users/{user_id}/role", response_model=UserOut)
def update_role(
    user_id: int,
    data: RoleUpdate,
    current: User = Depends(require_roles(Role.admin, Role.chief)),
    db: Session = Depends(get_db),
):
    """Смена роли пользователя.

    - admin может назначать роли eyewitness/inspector
    - назначать/снимать admin и chief может только chief
    """
    target = db.query(User).filter(User.id == user_id).first()
    if target is None:
        raise HTTPException(status_code=404, detail="Пользователь не найден")

    elevated = (Role.admin, Role.chief)
    # Если целевая или новая роль — административная, требуется chief
    if (data.role in elevated or target.role in elevated) and current.role != Role.chief:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Только начальник управляет ролями администраторов и начальников",
        )
    if target.id == current.id:
        raise HTTPException(status_code=400, detail="Нельзя менять собственную роль")

    target.role = data.role
    db.commit()
    db.refresh(target)
    return target


@router.delete("/users/{user_id}", response_model=UserOut)
def deactivate_user(
    user_id: int,
    current: User = Depends(require_roles(Role.chief)),
    db: Session = Depends(get_db),
):
    """Деактивация пользователя (мягкое удаление). Только начальник.

    Жёстко из БД не удаляем — иначе развалятся ссылки на инциденты/патрули.
    """
    target = db.query(User).filter(User.id == user_id).first()
    if target is None:
        raise HTTPException(status_code=404, detail="Пользователь не найден")
    if target.id == current.id:
        raise HTTPException(status_code=400, detail="Нельзя деактивировать самого себя")
    target.is_active = False
    db.commit()
    db.refresh(target)
    return target


@router.patch("/notifications", response_model=UserOut)
def toggle_my_notifications(
    data: NotificationsUpdate,
    current: User = Depends(require_roles(Role.admin, Role.chief)),
    db: Session = Depends(get_db),
):
    """Админ/начальник включает или выключает уведомления ТОЛЬКО для себя."""
    current.notifications_enabled = data.enabled
    db.commit()
    db.refresh(current)
    return current
