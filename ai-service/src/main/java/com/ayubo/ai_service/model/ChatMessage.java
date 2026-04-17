package com.ayubo.ai_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_history")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String patientId;
    private Long sessionId;
    private String sender;    // Will be "USER" or "AI"

    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDateTime timestamp;

    public ChatMessage() {}

    public ChatMessage(String patientId, Long sessionId, String sender, String message) {
        this.patientId = patientId;
        this.sessionId = sessionId;
        this.sender = sender;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getPatientId() { return patientId; }
    public Long getSessionId() { return sessionId; }
    public String getSender() { return sender; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
}