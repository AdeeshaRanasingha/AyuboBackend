package com.ayubo.telemedicine_service.service.impl;

import com.ayubo.telemedicine_service.config.TelemedicineSecurityProperties;
import com.ayubo.telemedicine_service.dto.SessionCreateRequest;
import com.ayubo.telemedicine_service.dto.SessionResponse;
import com.ayubo.telemedicine_service.dto.SessionStatusUpdateRequest;
import com.ayubo.telemedicine_service.entity.SessionStatus;
import com.ayubo.telemedicine_service.entity.TelemedicineSession;
import com.ayubo.telemedicine_service.exception.BadRequestException;
import com.ayubo.telemedicine_service.exception.ForbiddenException;
import com.ayubo.telemedicine_service.exception.ResourceNotFoundException;
import com.ayubo.telemedicine_service.repository.TelemedicineSessionRepository;
import com.ayubo.telemedicine_service.security.SecurityUtils;
import com.ayubo.telemedicine_service.service.TelemedicineSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TelemedicineSessionServiceImpl implements TelemedicineSessionService {

    private final TelemedicineSessionRepository telemedicineSessionRepository;
    private final TelemedicineSecurityProperties telemedicineSecurityProperties;

    @Value("${telemedicine.jitsi.base-url}")
    private String jitsiBaseUrl;

    @Override
    public SessionResponse createSession(SessionCreateRequest request) {
        telemedicineSessionRepository.findByAppointmentId(request.getAppointmentId()).ifPresent(existing -> {
            throw new BadRequestException("A telemedicine session already exists for this appointment");
        });

        Long doctorId = resolveDoctorIdForCaller(request.getDoctorId());
        String patientEmail = resolvePatientEmailForCaller(request.getPatientEmail());
        String roomName = buildRoomName(request.getAppointmentId(), doctorId);
        String joinUrl = buildJoinUrl(roomName);

        TelemedicineSession session = TelemedicineSession.builder()
                .appointmentId(request.getAppointmentId())
                .doctorId(doctorId)
                .patientId(request.getPatientId())
                .queueEntryId(request.getQueueEntryId())
                .patientEmail(patientEmail)
                .consultationType(normalizeConsultationType(request.getConsultationType()))
                .meetingProvider("JITSI")
                .roomName(roomName)
                .doctorJoinUrl(joinUrl)
                .patientJoinUrl(joinUrl)
                .status(SessionStatus.SCHEDULED)
                .scheduledAt(request.getScheduledAt())
                .notes("Session created")
                .build();

        return mapToResponse(telemedicineSessionRepository.save(session));
    }

    @Override
    public SessionResponse getSessionById(Long id) {
        TelemedicineSession session = findSessionById(id);
        assertCanAccessSession(session);
        return mapToResponse(session);
    }

    @Override
    public SessionResponse getSessionByAppointmentId(Long appointmentId) {
        TelemedicineSession session = telemedicineSessionRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Telemedicine session not found for appointment id: " + appointmentId));
        assertCanAccessSession(session);
        return mapToResponse(session);
    }

    @Override
    public List<SessionResponse> getMySessions() {
        if (SecurityUtils.hasRole("ADMIN")) {
            return telemedicineSessionRepository.findAll().stream().map(this::mapToResponse).toList();
        }
        if (SecurityUtils.hasRole("PATIENT")) {
            return telemedicineSessionRepository
                    .findByPatientEmailIgnoreCaseOrderByScheduledAtDesc(SecurityUtils.requireCurrentUserEmail())
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }
        if (SecurityUtils.hasRole("PROVIDER")) {
            Long doctorId = telemedicineSecurityProperties.doctorIdForProviderEmail(SecurityUtils.requireCurrentUserEmail())
                    .orElseThrow(() -> new ForbiddenException("No doctor profile mapped for this provider account"));
            return telemedicineSessionRepository.findByDoctorIdOrderByScheduledAtDesc(doctorId)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }
        throw new ForbiddenException("Unsupported role");
    }

    @Override
    public SessionResponse startSession(Long id, SessionStatusUpdateRequest request) {
        SecurityUtils.requireProviderOrAdmin();
        TelemedicineSession session = findSessionById(id);
        assertCanManageSession(session);

        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new BadRequestException("Only scheduled sessions can be started");
        }

        session.setStatus(SessionStatus.ACTIVE);
        session.setStartedAt(LocalDateTime.now());
        session.setNotes(request.getNotes());
        return mapToResponse(telemedicineSessionRepository.save(session));
    }

    @Override
    public SessionResponse endSession(Long id, SessionStatusUpdateRequest request) {
        SecurityUtils.requireProviderOrAdmin();
        TelemedicineSession session = findSessionById(id);
        assertCanManageSession(session);

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new BadRequestException("Only active sessions can be ended");
        }

        session.setStatus(SessionStatus.ENDED);
        session.setEndedAt(LocalDateTime.now());
        session.setNotes(request.getNotes());
        return mapToResponse(telemedicineSessionRepository.save(session));
    }

    private TelemedicineSession findSessionById(Long id) {
        return telemedicineSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Telemedicine session not found with id: " + id));
    }

    private void assertCanAccessSession(TelemedicineSession session) {
        if (SecurityUtils.hasRole("ADMIN")) {
            return;
        }

        String currentUserEmail = SecurityUtils.requireCurrentUserEmail();
        if (SecurityUtils.hasRole("PATIENT") && currentUserEmail.equalsIgnoreCase(session.getPatientEmail())) {
            return;
        }

        if (SecurityUtils.hasRole("PROVIDER")) {
            Long mappedDoctorId = telemedicineSecurityProperties.doctorIdForProviderEmail(currentUserEmail).orElse(null);
            if (mappedDoctorId != null && mappedDoctorId.equals(session.getDoctorId())) {
                return;
            }
        }

        throw new ForbiddenException("Not allowed to access this telemedicine session");
    }

    private void assertCanManageSession(TelemedicineSession session) {
        if (SecurityUtils.hasRole("ADMIN")) {
            return;
        }
        Long mappedDoctorId = telemedicineSecurityProperties.doctorIdForProviderEmail(SecurityUtils.requireCurrentUserEmail())
                .orElseThrow(() -> new ForbiddenException("No doctor profile mapped for this provider account"));
        if (!mappedDoctorId.equals(session.getDoctorId())) {
            throw new ForbiddenException("You cannot manage this telemedicine session");
        }
    }

    private Long resolveDoctorIdForCaller(Long requestedDoctorId) {
        if (SecurityUtils.hasRole("PROVIDER")) {
            Long mappedDoctorId = telemedicineSecurityProperties.doctorIdForProviderEmail(SecurityUtils.requireCurrentUserEmail())
                    .orElseThrow(() -> new ForbiddenException("No doctor profile mapped for this provider account"));
            if (!mappedDoctorId.equals(requestedDoctorId)) {
                throw new ForbiddenException("Provider account cannot create sessions for another doctor");
            }
            return mappedDoctorId;
        }
        return requestedDoctorId;
    }

    private String resolvePatientEmailForCaller(String requestedPatientEmail) {
        if (SecurityUtils.hasRole("PATIENT")) {
            return SecurityUtils.requireCurrentUserEmail();
        }
        if (requestedPatientEmail == null || requestedPatientEmail.isBlank()) {
            throw new BadRequestException("patientEmail is required when created by a provider or admin");
        }
        return requestedPatientEmail.trim();
    }

    private String normalizeConsultationType(String consultationType) {
        if (consultationType == null || consultationType.isBlank()) {
            return "VIDEO_CONSULTATION";
        }
        return consultationType.trim().toUpperCase(Locale.ROOT);
    }

    private String buildRoomName(Long appointmentId, Long doctorId) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "ayubo-" + appointmentId + "-" + doctorId + "-" + suffix;
    }

    private String buildJoinUrl(String roomName) {
        return jitsiBaseUrl.endsWith("/")
                ? jitsiBaseUrl + roomName
                : jitsiBaseUrl + "/" + roomName;
    }

    private SessionResponse mapToResponse(TelemedicineSession session) {
        return SessionResponse.builder()
                .id(session.getId())
                .appointmentId(session.getAppointmentId())
                .doctorId(session.getDoctorId())
                .patientId(session.getPatientId())
                .queueEntryId(session.getQueueEntryId())
                .patientEmail(session.getPatientEmail())
                .consultationType(session.getConsultationType())
                .meetingProvider(session.getMeetingProvider())
                .roomName(session.getRoomName())
                .doctorJoinUrl(session.getDoctorJoinUrl())
                .patientJoinUrl(session.getPatientJoinUrl())
                .status(session.getStatus().name())
                .scheduledAt(session.getScheduledAt())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
