"""Конфигурация приложения. Значения берутся из переменных окружения с дефолтами."""
import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent.parent

# Папка для загруженных медиафайлов (фото/видео от очевидцев)
MEDIA_DIR = BASE_DIR / "media"
MEDIA_DIR.mkdir(exist_ok=True)

# Путь к базе SQLite
DATABASE_URL = os.getenv("DATABASE_URL", f"sqlite:///{BASE_DIR / 'gibdd.db'}")

# JWT
SECRET_KEY = os.getenv("SECRET_KEY", "CHANGE_ME_IN_PRODUCTION_please_use_env_var")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES", "10080"))  # 7 дней

# Данные первого начальника (создаётся при сидинге, если БД пуста)
FIRST_CHIEF_PHONE = os.getenv("FIRST_CHIEF_PHONE", "+70000000000")
FIRST_CHIEF_PASSWORD = os.getenv("FIRST_CHIEF_PASSWORD", "admin123")
FIRST_CHIEF_NAME = os.getenv("FIRST_CHIEF_NAME", "Главный начальник")

# Лимит размера загружаемого файла (50 МБ)
MAX_UPLOAD_SIZE = 50 * 1024 * 1024
