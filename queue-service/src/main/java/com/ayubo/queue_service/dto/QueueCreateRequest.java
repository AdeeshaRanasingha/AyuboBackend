package com.ayubo.queue_service.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class QueueCreateRequest {
    @NotNull(message = "doctorId is required")
    private Long doctorId;

    private Long patientId;
    private Long appointmentId;

    @NotBlank(message = "patientName is required")
    private String patientName;

    private String specialty;

    @FutureOrPresent(message = "queueDate must be today or a future date")
    @NotNull(message = "queueDate is required")
    private LocalDate queueDate;

    private String estimatedTime;
    private String notes;
}
