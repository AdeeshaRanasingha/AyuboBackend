package com.ayubo.telemedicine_service.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SessionCreateRequest {

    @NotNull
    private Long appointmentId;

    @NotNull
    private Long doctorId;

    private Long patientId;

    private Long queueEntryId;

    private String patientEmail;

    private String consultationType;

    @NotNull
    @Future
    private LocalDateTime scheduledAt;
}
