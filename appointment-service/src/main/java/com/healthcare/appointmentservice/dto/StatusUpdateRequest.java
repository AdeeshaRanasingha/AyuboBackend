package com.healthcare.appointmentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StatusUpdateRequest {

    @NotBlank(message = "status is required")
    private String status;

    @Size(max = 255, message = "notes must not exceed 255 characters")
    private String notes;
}
