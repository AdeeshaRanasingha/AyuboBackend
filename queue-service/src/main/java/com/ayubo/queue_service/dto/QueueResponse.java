package com.ayubo.queue_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class QueueResponse {
    private Long id;
    private Long doctorId;
    private Long patientId;
    private Long appointmentId;
    private String patientEmail;
    private String patientName;
    private String specialty;
    private LocalDate queueDate;
    private Integer tokenNumber;
    private Integer queuePosition;
    private String estimatedTime;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
