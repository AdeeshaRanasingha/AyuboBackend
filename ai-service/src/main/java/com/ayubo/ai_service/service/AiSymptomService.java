package com.ayubo.ai_service.service;

import com.ayubo.ai_service.dto.AiRequest;
import com.ayubo.ai_service.dto.AiResponse;
import com.ayubo.ai_service.model.ChatMessage;
import com.ayubo.ai_service.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiSymptomService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Autowired
    private ChatMessageRepository chatRepository; // Inject the database!

    public AiResponse analyzeSymptoms(AiRequest request) {
        try {
            // 1. SAVE THE USER'S MESSAGE TO DB
            String patientId = request.getPatientId() != null ? request.getPatientId() : "anonymous";
            chatRepository.save(new ChatMessage(patientId, "USER", request.getMessage()));

            // 2. FETCH HISTORY & BUILD A TRANSCRIPT
            List<ChatMessage> history = chatRepository.findByPatientIdOrderByTimestampAsc(patientId);
            StringBuilder transcript = new StringBuilder();

            // We loop through the database history and format it like a script
            for (ChatMessage msg : history) {
                String role = msg.getSender().equals("USER") ? "Patient" : "AI Assistant";
                transcript.append(role).append(": ").append(msg.getMessage()).append("\n\n");
            }

            // The Direct Google API endpoint
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

            // 3. THE ULTIMATE SYSTEM INSTRUCTION (Now with the secret Specialty Tag!)
            String systemInstruction =
                    "You are the Ayubo AI Symptom Checker. You are an empathetic, highly intelligent medical assistant.\n\n" +
                            "INSTRUCTIONS:\n" +
                            "1. Read the 'Conversation Transcript' below to understand the full context of the patient's symptoms.\n" +
                            "2. If the patient's symptoms are vague, ask ONE follow-up question to clarify (e.g., severity, location, duration).\n" +
                            "3. Once you have enough information, STOP asking questions and provide a structured assessment.\n" +
                            "4. Give possible common causes (use phrases like 'This might be related to...'). Do NOT give a definitive diagnosis.\n" +
                            "5. Provide practical self-care advice.\n" +
                            "6. State clearly when they should seek professional medical help.\n" +
                            "7. AT THE VERY END OF YOUR RESPONSE, on a new line, write 'SPECIALTY_NEEDED: [Specialty]' (e.g., Cardiologist, General Practitioner, Dermatologist). If no doctor is needed yet, write 'SPECIALTY_NEEDED: None'.\n\n" +
                            "Conversation Transcript so far:\n" +
                            "--------------------------------\n" +
                            transcript.toString() +
                            "--------------------------------\n\n" +
                            "Please provide your next response as the AI Assistant:";

            // Note: Removed + request.getMessage() because the transcript already has it!
            String safePrompt = systemInstruction;

            // 4. Build the exact JSON structure Google expects
            Map<String, Object> part = new HashMap<>();
            part.put("text", safePrompt);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", Collections.singletonList(part));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", Collections.singletonList(content));

            // 5. Send the request directly to Google!
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Wait for the native response
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            // 6. Unpack the AI's reply from the JSON block
            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Gemini response body was empty");
            }

            List<Map<String, Object>> candidates = extractObjectList(body.get("candidates"));
            Map<String, Object> resContent = extractObjectMap(candidates.get(0).get("content"));
            List<Map<String, Object>> resParts = extractObjectList(resContent.get("parts"));
            String rawAiReply = String.valueOf(resParts.get(0).get("text"));

            // 7. EXTRACT THE SPECIALTY AND CLEAN THE MESSAGE
            String displayReply = rawAiReply;
            String specialty = "None";

            if (rawAiReply.contains("SPECIALTY_NEEDED:")) {
                String[] splitReply = rawAiReply.split("SPECIALTY_NEEDED:");
                displayReply = splitReply[0].trim(); // This is the clean message for the patient
                if (splitReply.length > 1) {
                    specialty = splitReply[1].trim();    // This is the hidden tag (e.g., "Cardiologist")
                }
            }

            // 8. SAVE THE CLEAN AI REPLY TO DB (We don't save the secret tag to the database!)
            chatRepository.save(new ChatMessage(patientId, "AI", displayReply));

            // 9. FETCH DOCTORS FROM YOUR DATABASE BASED ON SPECIALTY
            List<Map<String, String>> doctors = fetchDoctorsFromDatabase(specialty);

            // Return both the text and the list of doctors
            return new AiResponse(displayReply, doctors);

        } catch (Exception e) {
            System.err.println("AI FETCH ERROR: " + e.getMessage());
            // Return an empty array of doctors if it fails
            return new AiResponse("Sorry, I am having a bit of trouble connecting right now. Please try again!", Collections.emptyList());
        }
    }

    // NEW METHOD TO FETCH HISTORY
    public List<ChatMessage> getChatHistory(String patientId) {
        return chatRepository.findByPatientIdOrderByTimestampAsc(patientId);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> extractObjectList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }
        throw new IllegalStateException("Unexpected Gemini payload shape");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractObjectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalStateException("Unexpected Gemini payload shape");
    }

    // MOCK DATABASE CALL - Replace with your actual Doctor Repository later!
    private List<Map<String, String>> fetchDoctorsFromDatabase(String specialty) {
        if (specialty.equalsIgnoreCase("None") || specialty.isEmpty()) {
            return Collections.emptyList(); // Send no doctors if the AI says 'None'
        }

        // Just an example! If AI says "Cardiologist", it sends this doctor.
        if (specialty.toLowerCase().contains("cardiologist")) {
            return List.of(Map.of("name", "Dr. Kamal Perera", "specialty", "Cardiologist", "hospital", "Asiri Hospital"));
        } else if (specialty.toLowerCase().contains("general practitioner")) {
            return List.of(Map.of("name", "Dr. Nimali Silva", "specialty", "General Practitioner", "hospital", "Nawaloka Hospital"));
        } else if (specialty.toLowerCase().contains("dermatologist")) {
            return List.of(Map.of("name", "Dr. Saman Kumara", "specialty", "Dermatologist", "hospital", "Lanka Hospitals"));
        }

        // Default fallback doctor if the specialty isn't in our mock list but the AI suggested one
        return List.of(Map.of("name", "Dr. Amara Wijesinghe", "specialty", specialty, "hospital", "Hemas Hospital"));
    }
}
