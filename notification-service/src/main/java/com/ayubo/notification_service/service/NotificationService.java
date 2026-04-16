package com.ayubo.notification_service.service;

import com.ayubo.notification_service.dto.NotificationRequest;
import com.ayubo.notification_service.dto.NotificationResponse;
import com.ayubo.notification_service.model.Notification;
import com.ayubo.notification_service.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailSenderService emailSenderService;
    private final SmsSenderService smsSenderService;

    public NotificationResponse send(NotificationRequest request) {
        if (!StringUtils.hasText(request.getRecipientEmail()) && !StringUtils.hasText(request.getRecipientPhone())) {
            throw new IllegalArgumentException("Either recipientEmail or recipientPhone must be provided");
        }

        boolean emailSent = emailSenderService.sendEmail(request.getRecipientEmail(), request.getSubject(), request.getMessage());
        boolean smsSent = smsSenderService.sendSms(request.getRecipientPhone(), request.getMessage());

        Notification notification = new Notification();
        notification.setRecipientEmail(request.getRecipientEmail());
        notification.setRecipientPhone(request.getRecipientPhone());
        notification.setSubject(request.getSubject());
        notification.setMessage(request.getMessage());
        notification.setEmailSent(emailSent);
        notification.setSmsSent(smsSent);

        Notification saved = notificationRepository.save(notification);
        return map(saved);
    }

    public List<NotificationResponse> listForRecipient(String email, String phone) {
        if (StringUtils.hasText(email) && StringUtils.hasText(phone)) {
            return notificationRepository.findByRecipientEmailOrRecipientPhoneOrderByCreatedAtDesc(email, phone)
                    .stream()
                    .map(this::map)
                    .toList();
        }

        if (StringUtils.hasText(email)) {
            return notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(email)
                    .stream()
                    .map(this::map)
                    .toList();
        }

        if (StringUtils.hasText(phone)) {
            return notificationRepository.findByRecipientPhoneOrderByCreatedAtDesc(phone)
                    .stream()
                    .map(this::map)
                    .toList();
        }

        return Collections.emptyList();
    }

    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        return map(notificationRepository.save(notification));
    }

    public long unreadCount(String email, String phone) {
        long emailCount = StringUtils.hasText(email) ? notificationRepository.countByRecipientEmailAndReadFalse(email) : 0;
        long phoneCount = StringUtils.hasText(phone) ? notificationRepository.countByRecipientPhoneAndReadFalse(phone) : 0;
        return emailCount + phoneCount;
    }

    private NotificationResponse map(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .recipientEmail(n.getRecipientEmail())
                .recipientPhone(n.getRecipientPhone())
                .subject(n.getSubject())
                .message(n.getMessage())
                .read(n.isRead())
                .emailSent(n.isEmailSent())
                .smsSent(n.isSmsSent())
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .build();
    }
}
