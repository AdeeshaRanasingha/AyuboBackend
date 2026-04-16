package com.ayubo.telemedicine_service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class AppointmentResponse {
    private Long id;
    private String appointmentNumber;
    private Long patientId;
    private String patientEmail;
    private String appointmentFor;
    private String appointmentType;
    private String patientTitle;
    private String patientName;
    private String contactNumber;
    private String identificationType;
    private String identificationValue;
    private String contactEmail;
    private Long doctorId;
    private String specialty;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;
    private String noteOrAddress;
    private Boolean noShowRefund;
    private Boolean onGoingNumber;
    private String status;
    private String paymentStatus;
    private BigDecimal totalPrice;
    private String notes;
    private String cancelReason;
    private Integer rescheduleCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String prescriptionUrl;
}
