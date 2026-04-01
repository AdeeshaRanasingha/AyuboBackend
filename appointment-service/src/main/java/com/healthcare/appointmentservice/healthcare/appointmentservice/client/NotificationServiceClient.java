package com.healthcare.appointmentservice.healthcare.appointmentservice.client;

import com.healthcare.appointmentservice.healthcare.appointmentservice.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class NotificationServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.notification.base-url}")
    private String notificationServiceBaseUrl;

    public void sendNotification(NotificationRequest request) {
        try {
            String url = notificationServiceBaseUrl + "/api/notifications/send";
            restTemplate.postForObject(url, new HttpEntity<>(request), String.class);
        } catch (Exception ex) {
            System.out.println("Notification service unavailable: " + ex.getMessage());
        }
    }
}