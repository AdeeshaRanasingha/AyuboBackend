package com.ayubo.telemedicine_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SessionResponse {
    private Long id;
    private Long appointmentId;
    private Long doctorId;
    private Long patientId;
    private Long queueEntryId;
    private String patientEmail;
    private String consultationType;
    private String meetingProvider;
    private String roomName;
    private String doctorJoinUrl;
    private String patientJoinUrl;
    private String status;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
