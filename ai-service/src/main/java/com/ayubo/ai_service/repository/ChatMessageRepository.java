package com.ayubo.ai_service.repository;

import com.ayubo.ai_service.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // Spring automatically writes the SQL query for this method name!
    List<ChatMessage> findByPatientIdOrderByTimestampAsc(String patientId);
}