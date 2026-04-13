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
