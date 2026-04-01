package com.healthcare.appointmentservice.healthcare.appointmentservice.entity;

public enum AppointmentStatus {
    PENDING_PAYMENT,
    PENDING_CONFIRMATION,
    CONFIRMED,
    CANCELLED,
    RESCHEDULED,
    REJECTED,
    COMPLETED,
    NO_SHOW
}