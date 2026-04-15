package com.ayubo.queue_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QueueStatusUpdateRequest {
    @NotBlank(message = "status is required")
    private String status;

    private String notes;
}
