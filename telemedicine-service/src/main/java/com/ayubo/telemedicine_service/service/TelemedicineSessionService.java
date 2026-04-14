package com.ayubo.telemedicine_service.service;

import com.ayubo.telemedicine_service.dto.SessionCreateRequest;
import com.ayubo.telemedicine_service.dto.SessionResponse;
import com.ayubo.telemedicine_service.dto.SessionStatusUpdateRequest;

import java.util.List;

public interface TelemedicineSessionService {
    SessionResponse createSession(SessionCreateRequest request);
    SessionResponse getSessionById(Long id);
    SessionResponse getSessionByAppointmentId(Long appointmentId);
    List<SessionResponse> getMySessions();
    SessionResponse startSession(Long id, SessionStatusUpdateRequest request);
    SessionResponse endSession(Long id, SessionStatusUpdateRequest request);
}
