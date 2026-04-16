package com.ayubo.telemedicine_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SessionCreateRequest {

    @NotNull
    private Long appointmentId;
}
