# ГИБДД — бэкенд (FastAPI + SQLite)

Серверная часть системы оперативного уведомления о происшествиях.
Заменяет логику исходного Telegram-бота, отдаёт REST API для двух
мобильных приложений: клиента очевидца и клиента сотрудника ГИБДД.

## Роли

| Роль         | Возможности                                                                 |
|--------------|------------------------------------------------------------------------------|
| `eyewitness` | Очевидец. Без логина (вход по device_id). Отправляет инциденты с фото/видео/гео |
| `inspector`  | Инспектор. Логин/пароль. Кнопки «начать/закончить патруль», работа с инцидентами |
| `admin`      | + назначает роли (eyewitness/inspector), создаёт инспекторов, глушит уведомления себе |
| `chief`      | Начальник. Всё, что admin, + назначает/удаляет администраторов, деактивирует пользователей |

## Запуск

```bash
python3 -m venv .venv
source .venv/bin/activate        # Windows: .venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env             # отредактируй значения

uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

При первом запуске:
- создаются таблицы SQLite (файл `gibdd.db`);
- создаётся первый начальник с данными из `.env`
  (по умолчанию телефон `+70000000000`, пароль `admin123`).

Документация API (Swagger) — http://localhost:8000/docs

> **Важно для телефона:** `localhost` с телефона недоступен.
> Используй IP компьютера в локальной сети (например `http://192.168.1.50:8000`),
> а телефон и компьютер держи в одной Wi-Fi сети.
> Для эмулятора Android адрес хоста — `http://10.0.2.2:8000`.

## Карта API

### Авторизация
- `POST /auth/login` — вход сотрудника (форма OAuth2: username=телефон, password)
- `POST /auth/login-json` — то же, но JSON `{phone, password}` (для мобильного клиента)
- `POST /auth/eyewitness` — вход/регистрация очевидца `{device_id, full_name?}`
- `GET  /auth/me` — текущий пользователь

### Инциденты
- `POST /incidents` — создать (multipart: description, latitude, longitude, files[])
- `GET  /incidents/my` — свои инциденты (очевидец)
- `GET  /incidents?only_new=true` — лента для сотрудников
- `POST /incidents/{id}/accept` — принять в работу (нужен активный патруль)
- `POST /incidents/{id}/close` — закрыть

### Патруль
- `POST /patrol/start` — начать смену
- `POST /patrol/stop` — закончить смену
- `GET  /patrol/status` — на патруле или нет

### Администрирование
- `GET    /admin/users` — список пользователей (admin/chief)
- `POST   /admin/users` — создать сотрудника (admin/chief; admin/chief роли — только chief)
- `PATCH  /admin/users/{id}/role` — сменить роль
- `DELETE /admin/users/{id}` — деактивировать пользователя (только chief)
- `PATCH  /admin/notifications` — вкл/выкл уведомления для себя (admin/chief)

## Сброс БД

Удалить файл `gibdd.db` и перезапустить сервер — он создастся заново.

## Что дальше

Уведомления инспекторам сейчас работают через опрос (`GET /incidents?only_new=true`).
Мобильный клиент опрашивает эндпоинт раз в N секунд, пока инспектор на патруле.
При желании позже можно заменить на WebSocket или Firebase Cloud Messaging.
