package com.ayubo.ai_service.controller; // Change package to match your project

import com.ayubo.ai_service.dto.AiRequest;
import com.ayubo.ai_service.dto.AiResponse;
import com.ayubo.ai_service.service.AiSymptomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiSymptomService aiService;

    @PostMapping("/symptom-check")
    public ResponseEntity<AiResponse> checkSymptoms(@RequestBody AiRequest request) {

        // Ensure the user didn't send an empty message
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new AiResponse("Please describe your symptoms."));
        }

        AiResponse response = aiService.analyzeSymptoms(request);
        return ResponseEntity.ok(response);
    }
}