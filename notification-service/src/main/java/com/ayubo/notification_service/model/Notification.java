package com.ayubo.notification_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String recipientEmail;

    @Column(nullable = true)
    private String recipientPhone;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String message;

    /**
     * Persisted as {@code is_read} because {@code read} is a reserved word in MySQL.
     */
    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(nullable = false)
    private boolean emailSent;

    @Column(nullable = false)
    private boolean smsSent;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.read = false;
    }
}
