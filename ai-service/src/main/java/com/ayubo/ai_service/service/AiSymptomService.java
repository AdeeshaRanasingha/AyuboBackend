package com.ayubo.ai_service.service;

import com.ayubo.ai_service.dto.AiRequest;
import com.ayubo.ai_service.dto.AiResponse;
import com.ayubo.ai_service.model.ChatMessage;
import com.ayubo.ai_service.model.ChatSession;
import com.ayubo.ai_service.repository.ChatMessageRepository;
import com.ayubo.ai_service.repository.ChatSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Autowired
    private ChatMessageRepository chatRepository;

    @Autowired
    private ChatSessionRepository sessionRepository;

    public ChatSession createNewSession(String patientId) {
        ChatSession session = new ChatSession(patientId, "New Chat");
        return sessionRepository.save(session);
    }

    public List<ChatSession> getSessionsForPatient(String patientId) {
        return sessionRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public List<ChatMessage> getChatHistory(Long sessionId) {
        return chatRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }

    public AiResponse analyzeSymptoms(AiRequest request) {
        try {
            String patientId = request.getPatientId() != null ? request.getPatientId() : "anonymous";
            Long sessionId = request.getSessionId();
            System.out.println("[AI] patientId=" + patientId + " sessionId=" + sessionId + " key_present=" + (geminiApiKey != null && !geminiApiKey.isBlank()));

            // Auto-create a session if none provided
            if (sessionId == null) {
                ChatSession newSession = createNewSession(patientId);
                sessionId = newSession.getId();
                System.out.println("[AI] Auto-created session id=" + sessionId);
            }

            chatRepository.save(new ChatMessage(patientId, sessionId, "USER", request.getMessage()));
            System.out.println("[AI] Saved user message. Calling Gemini...");

            // Build transcript from this session only
            List<ChatMessage> history = chatRepository.findBySessionIdOrderByTimestampAsc(sessionId);
            StringBuilder transcript = new StringBuilder();
            for (ChatMessage msg : history) {
                String role = msg.getSender().equals("USER") ? "Patient" : "AI Assistant";
                transcript.append(role).append(": ").append(msg.getMessage()).append("\n\n");
            }

            // Update session title from first user message (truncated)
            if (history.size() == 1) {
                String title = request.getMessage().length() > 40
                        ? request.getMessage().substring(0, 40) + "..."
                        : request.getMessage();
                ChatSession session = sessionRepository.findById(sessionId).orElse(null);
                if (session != null) {
                    session.setTitle(title);
                    sessionRepository.save(session);
                }
            }

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + geminiApiKey;

            String systemInstruction =
                    "You are the Ayubo AI Symptom Checker — an empathetic, knowledgeable medical assistant for patients in Sri Lanka.\n\n" +
                            "RESPONSE RULES (follow every time):\n" +
                            "1. ALWAYS give a helpful, structured response even on the very first message. Never just ask a question and stop.\n" +
                            "2. Structure your response in clearly labelled sections using these exact headings:\n" +
                            "   🔍 Possible Causes\n" +
                            "   💊 Self-Care Tips\n" +
                            "   🚨 When to See a Doctor\n" +
                            "3. Under '🔍 Possible Causes', list 2-3 likely causes using phrases like 'This might be related to...'. Do NOT give a definitive diagnosis.\n" +
                            "4. Under '💊 Self-Care Tips', give 2-3 practical home remedies or actions the patient can take right now.\n" +
                            "5. Under '🚨 When to See a Doctor', describe 2-3 warning signs that mean they need professional help urgently.\n" +
                            "6. AFTER the three sections, ask ONE short follow-up question to better understand the symptoms (e.g., severity, duration, location). Keep this conversational and brief.\n" +
                            "7. AT THE VERY END, on a new line, write exactly: SPECIALTY_NEEDED: [Specialty] (e.g., General Practitioner, ENT Specialist, Cardiologist, Dermatologist, Neurologist). Even on the first message, make your best guess based on the symptoms. Only write 'SPECIALTY_NEEDED: None' if the symptom is clearly trivial.\n\n" +
                            "Conversation Transcript so far:\n" +
                            "--------------------------------\n" +
                            transcript.toString() +
                            "--------------------------------\n\n" +
                            "Please provide your next response as the AI Assistant:";

            Map<String, Object> part = new HashMap<>();
            part.put("text", systemInstruction);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", Collections.singletonList(part));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", Collections.singletonList(content));

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = null;
            int maxRetries = 4;
            int delayMs = 3000;
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    response = restTemplate.postForEntity(url, entity, Map.class);
                    break;
                } catch (org.springframework.web.client.HttpServerErrorException e) {
                    System.out.println("[AI] Attempt " + attempt + " failed (503). Retrying in " + delayMs + "ms...");
                    if (attempt == maxRetries) throw e;
                    Thread.sleep(delayMs);
                    delayMs *= 2;
                }
            }

            Map<String, Object> body = response.getBody();
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
            Map<String, Object> resContent = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> resParts = (List<Map<String, Object>>) resContent.get("parts");
            String rawAiReply = (String) resParts.get(0).get("text");

            String displayReply = rawAiReply;
            String specialty = "None";

            if (rawAiReply.contains("SPECIALTY_NEEDED:")) {
                String[] splitReply = rawAiReply.split("SPECIALTY_NEEDED:");
                displayReply = splitReply[0].trim();
                if (splitReply.length > 1) {
                    specialty = splitReply[1].trim();
                }
            }

            chatRepository.save(new ChatMessage(patientId, sessionId, "AI", displayReply));

            List<Map<String, String>> doctors = fetchDoctorsFromDatabase(specialty);

            return new AiResponse(displayReply, doctors);

        } catch (Exception e) {
            System.out.println("=== AI FETCH ERROR ===");
            System.out.println("Type: " + e.getClass().getName());
            System.out.println("Message: " + e.getMessage());
            e.printStackTrace(System.out);
            return new AiResponse("Sorry, I am having a bit of trouble connecting right now. Please try again!", Collections.emptyList());
        }
    }

    private List<Map<String, String>> fetchDoctorsFromDatabase(String specialty) {
        if (specialty.equalsIgnoreCase("None") || specialty.isEmpty()) {
            return Collections.emptyList();
        }

        if (specialty.toLowerCase().contains("cardiologist")) {
            return List.of(Map.of("name", "Dr. Kamal Perera", "specialty", "Cardiologist", "hospital", "Asiri Hospital"));
        } else if (specialty.toLowerCase().contains("general practitioner")) {
            return List.of(Map.of("name", "Dr. Nimali Silva", "specialty", "General Practitioner", "hospital", "Nawaloka Hospital"));
        } else if (specialty.toLowerCase().contains("dermatologist")) {
            return List.of(Map.of("name", "Dr. Saman Kumara", "specialty", "Dermatologist", "hospital", "Lanka Hospitals"));
        }

        return List.of(Map.of("name", "Dr. Amara Wijesinghe", "specialty", specialty, "hospital", "Hemas Hospital"));
    }
}
