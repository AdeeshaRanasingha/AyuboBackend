package com.ayubo.payment_service.dto;

public class PaymentRequest {

    private Long appointmentId;
    private Double amount;
    private String patientEmail;

    // Default constructor (Spring Boot needs this to convert React's JSON)
    public PaymentRequest() {}

    // --- Getters and Setters ---
    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getPatientEmail() {
        return patientEmail;
    }

    public void setPatientEmail(String patientEmail) {
        this.patientEmail = patientEmail;
    }
}
