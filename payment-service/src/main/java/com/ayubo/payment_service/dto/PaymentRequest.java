package com.ayubo.payment_service.dto;

public class PaymentRequest {

    private Long appointmentId;

    public PaymentRequest() {
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }
}