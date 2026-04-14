package com.ayubo.telemedicine_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SessionStatusUpdateRequest {
    @NotBlank
    private String notes;
}
