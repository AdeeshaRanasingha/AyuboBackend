package com.ayubo.ai_service.repository;

import com.ayubo.ai_service.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByPatientIdOrderByCreatedAtDesc(String patientId);
}
