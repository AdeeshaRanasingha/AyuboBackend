package com.ayubo.telemedicine_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DevTokenRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String role;
}
