# Appointment Service

## Features
- Create appointment
- View appointment by ID
- View all appointments
- View appointments by patient
- View appointments by doctor
- Update/reschedule appointment
- Cancel appointment
- Update appointment status
- Check doctor available slots
- Send notification request to Notification Service

## Tech Stack
- Spring Boot
- Spring Data JPA
- Spring Security
- MySQL
- Docker
- Swagger

## Run locally

### 1. Create MySQL DB
Database name: `appointment_db`

### 2. Update `application.yml`
Set your MySQL username/password.

### 3. Build project
```bash
mvn clean package