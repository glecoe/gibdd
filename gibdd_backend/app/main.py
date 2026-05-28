"""Точка входа FastAPI.

Запуск:
    uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

При первом старте создаёт таблицы и первого начальника (chief),
если в БД ещё нет ни одного сотрудника.
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from app.core.config import (
    MEDIA_DIR, FIRST_CHIEF_PHONE, FIRST_CHIEF_PASSWORD, FIRST_CHIEF_NAME,
)
from app.core.database import Base, engine, SessionLocal
from app.core.security import hash_password
from app.core.push import init_firebase
from app.models.models import User, Role
from app.routers import auth, incidents, patrol, admin

app = FastAPI(title="ГИБДД — оперативное уведомление", version="1.0.0")

# CORS: для разработки разрешаем всё. В проде сузить до адресов клиентов.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Раздача загруженных медиафайлов
app.mount("/media", StaticFiles(directory=str(MEDIA_DIR)), name="media")

app.include_router(auth.router)
app.include_router(incidents.router)
app.include_router(patrol.router)
app.include_router(admin.router)


def init_db():
    """Создаёт таблицы и первого начальника, если сотрудников ещё нет."""
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    try:
        has_staff = db.query(User).filter(
            User.role.in_([Role.chief, Role.admin, Role.inspector])
        ).first()
        if has_staff is None:
            chief = User(
                phone=FIRST_CHIEF_PHONE,
                password_hash=hash_password(FIRST_CHIEF_PASSWORD),
                full_name=FIRST_CHIEF_NAME,
                role=Role.chief,
            )
            db.add(chief)
            db.commit()
            print(f"[seed] Создан начальник: {FIRST_CHIEF_PHONE} / {FIRST_CHIEF_PASSWORD}")
    finally:
        db.close()


@app.on_event("startup")
def on_startup():
    init_db()
    init_firebase()


@app.get("/")
def root():
    return {"status": "ok", "service": "gibdd-api"}
