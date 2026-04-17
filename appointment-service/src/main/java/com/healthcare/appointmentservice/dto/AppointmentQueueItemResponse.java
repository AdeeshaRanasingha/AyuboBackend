package com.healthcare.appointmentservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class AppointmentQueueItemResponse {
    private Long id;
    private String appointmentNumber;
    private Long doctorId;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private String appointmentType;
    private String status;
    private String paymentStatus;
    private LocalDateTime createdAt;
}
