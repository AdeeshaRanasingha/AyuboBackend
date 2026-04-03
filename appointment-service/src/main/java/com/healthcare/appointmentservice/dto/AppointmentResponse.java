package com.healthcare.appointmentservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class AppointmentResponse {

    private Long id;
    private String appointmentNumber;
    private Long patientId;
    private String patientEmail;
    private Long doctorId;
    private String specialty;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;
    private String status;
    private String paymentStatus;
    private String meetingLink;
    private String notes;
    private String cancelReason;
    private Integer rescheduleCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
