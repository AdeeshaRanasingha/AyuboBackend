package com.ayubo.ai_service.dto;

import java.util.List;
import java.util.Map;

public class AiResponse {
    private String reply;
    // Adding a list of maps to hold doctor details (Name, Specialty, Image, etc.)
    private List<Map<String, String>> recommendedDoctors;

    public AiResponse() {}

    public AiResponse(String reply, List<Map<String, String>> recommendedDoctors) {
        this.reply = reply;
        this.recommendedDoctors = recommendedDoctors;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public List<Map<String, String>> getRecommendedDoctors() { return recommendedDoctors; }
    public void setRecommendedDoctors(List<Map<String, String>> recommendedDoctors) { this.recommendedDoctors = recommendedDoctors; }
}