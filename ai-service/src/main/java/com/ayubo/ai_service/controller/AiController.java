package com.ayubo.ai_service.controller;

import com.ayubo.ai_service.dto.AiRequest;
import com.ayubo.ai_service.dto.AiResponse;
import com.ayubo.ai_service.model.ChatMessage;
import com.ayubo.ai_service.model.ChatSession;
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

    @PostMapping("/symptom-check")
    public ResponseEntity<?> symptomCheck(@RequestBody AiRequest request) {
        try {
            AiResponse response = aiService.analyzeSymptoms(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "AI processing failed: " + e.getMessage())
            );
        }
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@RequestBody Map<String, String> body) {
        try {
            String patientId = body.get("patientId");
            ChatSession session = aiService.createNewSession(patientId);
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Failed to create session: " + e.getMessage())
            );
        }
    }

    @GetMapping("/sessions/{patientId}")
    public ResponseEntity<List<ChatSession>> getSessions(@PathVariable String patientId) {
        try {
            List<ChatSession> sessions = aiService.getSessionsForPatient(patientId);
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessage>> getHistory(@PathVariable Long sessionId) {
        try {
            List<ChatMessage> history = aiService.getChatHistory(sessionId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
