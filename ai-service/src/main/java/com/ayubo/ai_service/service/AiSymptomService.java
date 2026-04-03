package com.ayubo.ai_service.service;

import com.ayubo.ai_service.dto.AiRequest;
import com.ayubo.ai_service.dto.AiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiSymptomService {

    private String geminiApiKey = "AIzaSyD37wO45r9IfTKXd20vSAL8YHFuF9s1_f4";

    public AiResponse analyzeSymptoms(AiRequest request) {
        try {
            // 1. The Direct Google API endpoint
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

            // 2. The Medical Guardrail Prompt
            String systemInstruction = "You are the Ayubo AI Symptom Checker. Be concise, empathetic, and professional. NEVER give an official medical diagnosis. Patient's message: ";
            String safePrompt = systemInstruction + request.getMessage();

            // 3. Build the exact JSON structure Google expects
            Map<String, Object> part = new HashMap<>();
            part.put("text", safePrompt);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", Collections.singletonList(part));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", Collections.singletonList(content));

            // 4. Send the request directly to Google!
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Wait for the native response
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            // 5. Unpack the AI's reply from the JSON block
            Map<String, Object> body = response.getBody();
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
            Map<String, Object> resContent = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> resParts = (List<Map<String, Object>>) resContent.get("parts");
            String aiReply = (String) resParts.get(0).get("text");

            return new AiResponse(aiReply);

        } catch (Exception e) {
            System.err.println("AI FETCH ERROR: " + e.getMessage());
            return new AiResponse("Sorry, I am having a bit of trouble connecting right now. Please try again!");
        }
    }
}