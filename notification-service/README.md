# Notification Service

This microservice handles email/SMS delivery and stores in-app notification history.

## Port

- `8086`

## Endpoints

- `POST /api/notifications/send`
- `GET /api/notifications/my?email={email}&phone={phone}`
- `PATCH /api/notifications/{id}/read`
- `GET /api/notifications/unread-count?email={email}&phone={phone}`

## Third-party integrations

- Email: SMTP (`spring.mail.*`)
- SMS: Twilio REST API

Both integrations can be toggled by environment variables:

- `NOTIFICATION_EMAIL_ENABLED=true|false`
- `NOTIFICATION_SMS_ENABLED=true|false`

## Run

```bash
mvn spring-boot:run
```

### MySQL: `read` column error

`read` is reserved in MySQL. The entity maps the field to column **`is_read`**. If you ever see a failed migration, drop the table and restart (dev only):

```sql
DROP TABLE IF EXISTS notifications;
```

### Run with SMTP + Twilio env vars (recommended)

1. Copy `run-notification.local.ps1.example` → `run-notification.local.ps1`
2. Put your Gmail App Password + Twilio SID/token/phone (or Messaging Service SID) in **`run-notification.local.ps1`** only (this file is gitignored).
3. Start:
   - **PowerShell:** `.\run-notification.ps1`
   - **Git Bash:** `chmod +x run-notification.sh` then `./run-notification.sh`  
     Prints `Launching run-notification.ps1 via PowerShell...` then starts Maven on port **8086**.

Environment variables supported:

- `TWILIO_FROM_NUMBER` — your Twilio sender number (E.164), **or**
- `TWILIO_MESSAGING_SERVICE_SID` — `MG...` from Twilio “Messaging Service” (tutorial); use this **or** `FROM`, not both required.

### Twilio trial account (SMS not sending)

Trial accounts can only SMS **verified** destination numbers.

1. Twilio Console → **Phone Numbers** → **Verified Caller IDs** (or “Verified numbers”) → add `+94...` and complete verification.
2. Retry `POST /api/notifications/send` — check response `smsSent: true`.
3. If it still fails, read the service console log: it prints Twilio HTTP error JSON (e.g. unverified number).

### Security

Never commit `run-notification.local.ps1`. If tokens were shared or committed, **rotate** Twilio Auth Token and Gmail App Password in the provider consoles.
