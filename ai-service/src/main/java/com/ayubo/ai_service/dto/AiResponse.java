package com.ayubo.ai_service.dto; // Change package to match your project

public class AiResponse {
    private String reply;

    public AiResponse(String reply) {
        this.reply = reply;
    }

    // Getters and Setters
    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }
}