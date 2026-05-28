"""Модели базы данных.

Роли в системе:
  - eyewitness — очевидец (без авторизации, регистрируется по device_id)
  - inspector  — инспектор ГИБДД (логин/пароль), может патрулировать
  - admin      — администратор: назначает роли/инспекторов, управляет своими уведомлениями
  - chief      — начальник: всё что admin + назначает/удаляет администраторов
"""
import enum
from datetime import datetime, timezone

from sqlalchemy import (
    Column, Integer, String, DateTime, Boolean, ForeignKey, Enum, Text, Float
)
from sqlalchemy.orm import relationship

from app.core.database import Base


def utcnow():
    return datetime.now(timezone.utc)


class Role(str, enum.Enum):
    eyewitness = "eyewitness"
    inspector = "inspector"
    admin = "admin"
    chief = "chief"


class IncidentStatus(str, enum.Enum):
    new = "new"            # только что создан очевидцем
    accepted = "accepted"  # инспектор принял в работу
    closed = "closed"      # обработан


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    # Телефон — логин для сотрудников. У очевидцев может быть пустым.
    phone = Column(String, unique=True, nullable=True, index=True)
    password_hash = Column(String, nullable=True)  # есть только у сотрудников
    full_name = Column(String, nullable=True)
    role = Column(Enum(Role), default=Role.eyewitness, nullable=False)

    # Идентификатор устройства — для очевидцев, чтобы привязать без логина
    device_id = Column(String, unique=True, nullable=True, index=True)

    # Управляется самим пользователем (admin/chief глушат уведомления только себе)
    notifications_enabled = Column(Boolean, default=True, nullable=False)

    is_active = Column(Boolean, default=True, nullable=False)
    created_at = Column(DateTime, default=utcnow, nullable=False)

    # Токен устройства для пуш-уведомлений (Firebase Cloud Messaging)
    fcm_token = Column(String, nullable=True)

    incidents = relationship("Incident", back_populates="author", foreign_keys="Incident.author_id")
    patrols = relationship("Patrol", back_populates="inspector")


class Incident(Base):
    """Сообщение очевидца о пьяном водителе / происшествии."""
    __tablename__ = "incidents"

    id = Column(Integer, primary_key=True, index=True)
    author_id = Column(Integer, ForeignKey("users.id"), nullable=False)

    description = Column(Text, nullable=True)
    latitude = Column(Float, nullable=True)
    longitude = Column(Float, nullable=True)

    status = Column(Enum(IncidentStatus), default=IncidentStatus.new, nullable=False)
    # Инспектор, принявший инцидент в работу
    accepted_by_id = Column(Integer, ForeignKey("users.id"), nullable=True)

    created_at = Column(DateTime, default=utcnow, nullable=False)

    author = relationship("User", back_populates="incidents", foreign_keys=[author_id])
    accepted_by = relationship("User", foreign_keys=[accepted_by_id])
    media = relationship("MediaFile", back_populates="incident", cascade="all, delete-orphan")


class MediaFile(Base):
    """Фото или видео, прикреплённое к инциденту."""
    __tablename__ = "media_files"

    id = Column(Integer, primary_key=True, index=True)
    incident_id = Column(Integer, ForeignKey("incidents.id"), nullable=False)
    filename = Column(String, nullable=False)        # имя файла на диске
    original_name = Column(String, nullable=True)
    media_type = Column(String, nullable=True)       # "photo" / "video"
    created_at = Column(DateTime, default=utcnow, nullable=False)

    incident = relationship("Incident", back_populates="media")


class Patrol(Base):
    """Смена патрулирования инспектора. Пока есть незакрытый патруль — инспектор 'на смене'."""
    __tablename__ = "patrols"

    id = Column(Integer, primary_key=True, index=True)
    inspector_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    started_at = Column(DateTime, default=utcnow, nullable=False)
    ended_at = Column(DateTime, nullable=True)  # None => патруль активен

    inspector = relationship("User", back_populates="patrols")
