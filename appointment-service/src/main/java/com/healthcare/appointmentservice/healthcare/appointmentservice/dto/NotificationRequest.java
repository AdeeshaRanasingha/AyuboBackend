package com.healthcare.appointmentservice.healthcare.appointmentservice.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationRequest {

    private String recipientEmail;
    private String recipientPhone;
    private String subject;
    private String message;
}