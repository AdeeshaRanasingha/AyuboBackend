package com.ayubo.ai_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_sessions")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String patientId;

    private String title;

    private LocalDateTime createdAt;

    public ChatSession() {}

    public ChatSession(String patientId, String title) {
        this.patientId = patientId;
        this.title = title;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getPatientId() { return patientId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
