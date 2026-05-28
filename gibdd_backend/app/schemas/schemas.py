"""Pydantic-схемы запросов и ответов."""
from datetime import datetime
from typing import Optional, List

from pydantic import BaseModel

from app.models.models import Role, IncidentStatus


# ---------- Авторизация ----------
class LoginRequest(BaseModel):
    phone: str
    password: str


class EyewitnessRegisterRequest(BaseModel):
    """Очевидец 'регистрируется' по идентификатору устройства, без пароля."""
    device_id: str
    full_name: Optional[str] = None


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: "UserOut"


# ---------- Пользователи ----------
class UserOut(BaseModel):
    id: int
    phone: Optional[str]
    full_name: Optional[str]
    role: Role
    notifications_enabled: bool
    is_active: bool
    created_at: datetime

    class Config:
        from_attributes = True


class UserCreate(BaseModel):
    """Создание сотрудника админом/начальником."""
    phone: str
    password: str
    full_name: Optional[str] = None
    role: Role = Role.inspector


class RoleUpdate(BaseModel):
    role: Role


class NotificationsUpdate(BaseModel):
    enabled: bool


class FcmTokenUpdate(BaseModel):
    fcm_token: str


# ---------- Медиа ----------
class MediaOut(BaseModel):
    id: int
    filename: str
    media_type: Optional[str]
    url: str  # вычисляется на лету в роутере

    class Config:
        from_attributes = True


# ---------- Инциденты ----------
class IncidentOut(BaseModel):
    id: int
    author_id: int
    description: Optional[str]
    latitude: Optional[float]
    longitude: Optional[float]
    status: IncidentStatus
    accepted_by_id: Optional[int]
    created_at: datetime
    media: List[MediaOut] = []

    class Config:
        from_attributes = True


# ---------- Патрули ----------
class PatrolOut(BaseModel):
    id: int
    inspector_id: int
    started_at: datetime
    ended_at: Optional[datetime]

    class Config:
        from_attributes = True


TokenResponse.model_rebuild()
