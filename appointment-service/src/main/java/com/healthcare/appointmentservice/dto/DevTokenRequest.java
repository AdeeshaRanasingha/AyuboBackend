package com.healthcare.appointmentservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DevTokenRequest {

    @NotBlank
    private String email;

    /**
     * PATIENT or PROVIDER (same as auth-service).
     */
    @NotBlank
    private String role;
}
