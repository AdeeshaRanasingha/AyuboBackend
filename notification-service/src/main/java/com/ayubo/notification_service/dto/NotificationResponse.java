package com.ayubo.notification_service.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class NotificationResponse {
    Long id;
    String recipientEmail;
    String recipientPhone;
    String subject;
    String message;
    boolean read;
    boolean emailSent;
    boolean smsSent;
    LocalDateTime createdAt;
    LocalDateTime readAt;
}
