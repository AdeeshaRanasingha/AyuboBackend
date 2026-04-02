package com.ayubo.ai_service.service; // Change package to match your project

import com.ayubo.ai_service.dto.AiRequest;
import com.ayubo.ai_service.dto.AiResponse;
import org.springframework.stereotype.Service;

@Service
public class AiSymptomService {

    public AiResponse analyzeSymptoms(AiRequest request) {
        String userMessage = request.getMessage().toLowerCase();
        String aiReply;

        // ==========================================
        // TODO LATER: REAL AI INTEGRATION GOES HERE
        // You would use RestTemplate or WebClient to send
        // 'userMessage' to the OpenAI or Gemini API here.
        // ==========================================

        // For now, we use an advanced mock logic
        if (userMessage.contains("headache") || userMessage.contains("migraine")) {
            aiReply = "I'm sorry to hear about your headache. If it's sudden and severe, or accompanied by vision changes, please visit an emergency room. Otherwise, booking a consultation with a Neurologist or General Practitioner is recommended.";
        }
        else if (userMessage.contains("fever") || userMessage.contains("temperature")) {
            aiReply = "A fever indicates your body is fighting off an infection. Please stay hydrated and rest. If your temperature exceeds 39°C (102.2°F) or lasts more than 3 days, please book a Video Consult with a General Practitioner.";
        }
        else if (userMessage.contains("chest") || userMessage.contains("heart")) {
            aiReply = "🚨 WARNING: Chest pain can be a sign of a medical emergency, such as a heart attack. Please do NOT wait. Call 1990 or go to the nearest hospital immediately.";
        }
        else if (userMessage.contains("stomach") || userMessage.contains("nausea")) {
            aiReply = "Stomach pain and nausea can have many causes, from food poisoning to viral infections. Make sure to drink plenty of clear fluids. A General Practitioner or Gastroenterologist can help diagnose this.";
        }
        else {
            aiReply = "Thank you for sharing your symptoms. While I am an AI and cannot diagnose you, I recommend booking a consultation with a General Practitioner to get a proper medical evaluation. Would you like to see available doctors?";
        }

        return new AiResponse(aiReply);
    }
}