package com.ayubo.ai_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_history")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String patientId; // To know whose history this is
    private String sender;    // Will be "USER" or "AI"

    @Column(columnDefinition = "TEXT") // TEXT because AI replies can be long
    private String message;

    private LocalDateTime timestamp;

    // Default Constructor
    public ChatMessage() {}

    // Constructor for easy saving
    public ChatMessage(String patientId, String sender, String message) {
        this.patientId = patientId;
        this.sender = sender;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public String getPatientId() { return patientId; }
    public String getSender() { return sender; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
}