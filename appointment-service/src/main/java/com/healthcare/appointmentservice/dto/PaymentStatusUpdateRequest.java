package com.healthcare.appointmentservice.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentStatusUpdateRequest {
    private String paymentStatus;
    private BigDecimal totalPrice;
    private String notes;
}