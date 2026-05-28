"""Отправка пуш-уведомлений через Firebase Cloud Messaging.

Работает в двух режимах:
  1. Если задан GOOGLE_APPLICATION_CREDENTIALS (путь к service-account JSON
     из Firebase) и установлен firebase-admin — шлёт реальные пуши.
  2. Иначе — просто пишет в лог. Это позволяет бэкенду и всему остальному
     функционалу работать даже без настроенного Firebase (например, пока
     уведомления идут через опрос сервера).
"""
import logging
import os

logger = logging.getLogger("push")

_firebase_app = None
_firebase_ready = False


def init_firebase():
    """Пытается инициализировать Firebase Admin SDK. Безопасно при отсутствии настроек."""
    global _firebase_app, _firebase_ready
    cred_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    if not cred_path or not os.path.exists(cred_path):
        logger.info("FCM не настроен (нет GOOGLE_APPLICATION_CREDENTIALS) — пуши отключены")
        return
    try:
        import firebase_admin
        from firebase_admin import credentials

        cred = credentials.Certificate(cred_path)
        _firebase_app = firebase_admin.initialize_app(cred)
        _firebase_ready = True
        logger.info("Firebase Admin SDK инициализирован — пуши включены")
    except Exception as exc:  # noqa: BLE001
        logger.warning("Не удалось инициализировать Firebase: %s", exc)


def send_push(token: str, title: str, body: str, data: dict | None = None):
    """Отправляет один пуш. Если Firebase не готов — логирует и выходит."""
    if not token:
        return
    if not _firebase_ready:
        logger.info("[push:mock] -> %s | %s: %s", token[:12], title, body)
        return
    try:
        from firebase_admin import messaging

        message = messaging.Message(
            notification=messaging.Notification(title=title, body=body),
            data={k: str(v) for k, v in (data or {}).items()},
            token=token,
        )
        messaging.send(message)
    except Exception as exc:  # noqa: BLE001
        logger.warning("Ошибка отправки пуша: %s", exc)
