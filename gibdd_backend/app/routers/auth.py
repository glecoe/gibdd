"""Авторизация: вход сотрудников по телефону/паролю и регистрация очевидцев по device_id."""
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.security import verify_password, create_access_token
from app.core.deps import get_current_user
from app.models.models import User, Role
from app.schemas.schemas import (
    TokenResponse, EyewitnessRegisterRequest, UserOut, LoginRequest, FcmTokenUpdate,
)

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/login", response_model=TokenResponse)
def login(form: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
    """Вход сотрудника. username == телефон, password == пароль.

    Используем стандартную форму OAuth2, чтобы работала кнопка Authorize в Swagger.
    """
    user = db.query(User).filter(User.phone == form.username).first()
    if not user or not user.password_hash or not verify_password(form.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Неверный телефон или пароль",
        )
    if not user.is_active:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Учётная запись отключена")
    token = create_access_token(user.id)
    return TokenResponse(access_token=token, user=UserOut.model_validate(user))


@router.post("/login-json", response_model=TokenResponse)
def login_json(data: LoginRequest, db: Session = Depends(get_db)):
    """То же, что /login, но принимает JSON — удобнее для мобильного клиента."""
    user = db.query(User).filter(User.phone == data.phone).first()
    if not user or not user.password_hash or not verify_password(data.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Неверный телефон или пароль",
        )
    if not user.is_active:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Учётная запись отключена")
    token = create_access_token(user.id)
    return TokenResponse(access_token=token, user=UserOut.model_validate(user))


@router.post("/eyewitness", response_model=TokenResponse)
def register_eyewitness(data: EyewitnessRegisterRequest, db: Session = Depends(get_db)):
    """Очевидец входит/регистрируется по идентификатору устройства.

    Если устройство уже встречалось — возвращаем того же пользователя.
    Логина и пароля нет — анонимная отправка инцидентов.
    """
    user = db.query(User).filter(User.device_id == data.device_id).first()
    if user is None:
        user = User(
            device_id=data.device_id,
            full_name=data.full_name,
            role=Role.eyewitness,
        )
        db.add(user)
        db.commit()
        db.refresh(user)
    token = create_access_token(user.id)
    return TokenResponse(access_token=token, user=UserOut.model_validate(user))


@router.get("/me", response_model=UserOut)
def me(current: User = Depends(get_current_user)):
    return current


@router.post("/fcm-token", response_model=UserOut)
def update_fcm_token(
    data: FcmTokenUpdate,
    current: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Сохраняет/обновляет FCM-токен устройства для пуш-уведомлений."""
    current.fcm_token = data.fcm_token
    db.commit()
    db.refresh(current)
    return current
