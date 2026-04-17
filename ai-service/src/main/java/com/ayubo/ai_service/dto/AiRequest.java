package com.ayubo.ai_service.dto;

public class AiRequest {
    private String message;
    private String patientId;
    private Long sessionId;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
}