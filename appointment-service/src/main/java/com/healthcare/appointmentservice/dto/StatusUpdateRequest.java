package com.healthcare.appointmentservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StatusUpdateRequest {

    @NotBlank(message = "status is required")
    private String status;

}
