package com.ayubo.notification_service.repository;

import com.ayubo.notification_service.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);

    List<Notification> findByRecipientPhoneOrderByCreatedAtDesc(String recipientPhone);

    List<Notification> findByRecipientEmailOrRecipientPhoneOrderByCreatedAtDesc(String recipientEmail, String recipientPhone);

    long countByRecipientEmailAndReadFalse(String recipientEmail);

    long countByRecipientPhoneAndReadFalse(String recipientPhone);
}
