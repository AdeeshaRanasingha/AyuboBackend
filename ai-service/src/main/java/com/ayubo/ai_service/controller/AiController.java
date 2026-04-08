package com.ayubo.ai_service.controller;

import com.ayubo.ai_service.dto.AiRequest;
import com.ayubo.ai_service.dto.AiResponse;
import com.ayubo.ai_service.model.ChatMessage;
import com.ayubo.ai_service.service.AiSymptomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")

public class AiController {

    @Autowired
    private AiSymptomService aiService;

    // 1. Endpoint to send messages to the AI
    @PostMapping("/symptom-check")
    public ResponseEntity<?> symptomCheck(@RequestBody AiRequest request) {
        try {
            // FIXED: Using the injected 'aiService' and the correct method name 'analyzeSymptoms'
            AiResponse response = aiService.analyzeSymptoms(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "AI processing failed: " + e.getMessage())
            );
        }
    }

    // 2. NEW Endpoint to fetch chat history when the patient opens the page
    @GetMapping("/history/{patientId}")
    public ResponseEntity<List<ChatMessage>> getHistory(@PathVariable String patientId) {
        try {
            List<ChatMessage> history = aiService.getChatHistory(patientId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}