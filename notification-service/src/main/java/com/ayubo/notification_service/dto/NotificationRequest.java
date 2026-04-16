package com.ayubo.notification_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NotificationRequest {

    private String recipientEmail;
    private String recipientPhone;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Message is required")
    private String message;
}
