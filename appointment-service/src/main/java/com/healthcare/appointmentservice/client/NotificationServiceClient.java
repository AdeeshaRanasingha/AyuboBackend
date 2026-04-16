package com.healthcare.appointmentservice.client;

import com.healthcare.appointmentservice.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForObject(url, new HttpEntity<>(request, headers), String.class);
        } catch (Exception ex) {
            System.out.println("Notification service unavailable: " + ex.getMessage());
        }
    }
}
