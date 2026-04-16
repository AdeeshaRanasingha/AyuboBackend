package com.ayubo.telemedicine_service.service.impl;

import com.ayubo.telemedicine_service.dto.SessionCreateRequest;
import com.ayubo.telemedicine_service.dto.SessionResponse;
import com.ayubo.telemedicine_service.dto.SessionStatusUpdateRequest;
import com.ayubo.telemedicine_service.dto.AppointmentResponse;
import com.ayubo.telemedicine_service.entity.SessionStatus;
import com.ayubo.telemedicine_service.entity.TelemedicineSession;
import com.ayubo.telemedicine_service.exception.BadRequestException;
import com.ayubo.telemedicine_service.exception.ForbiddenException;
import com.ayubo.telemedicine_service.exception.ResourceNotFoundException;
import com.ayubo.telemedicine_service.repository.TelemedicineSessionRepository;
import com.ayubo.telemedicine_service.security.SecurityUtils;
import com.ayubo.telemedicine_service.service.AppointmentServiceClient;
import com.ayubo.telemedicine_service.service.ProviderDoctorResolver;
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
    private final AppointmentServiceClient appointmentServiceClient;
    private final ProviderDoctorResolver providerDoctorResolver;

    @Value("${telemedicine.jitsi.base-url}")
    private String jitsiBaseUrl;

    @Override
    public SessionResponse createSession(SessionCreateRequest request) {
        if (!SecurityUtils.hasRole("PROVIDER")) {
            throw new ForbiddenException("Provider role required");
        }

        telemedicineSessionRepository.findByAppointmentId(request.getAppointmentId()).ifPresent(existing -> {
            throw new BadRequestException("A telemedicine session already exists for this appointment");
        });

        AppointmentResponse appointment = appointmentServiceClient.getAppointmentById(request.getAppointmentId());
        if (appointment == null) {
            throw new ResourceNotFoundException("Appointment not found with id: " + request.getAppointmentId());
        }

        assertAppointmentIsTelemedicineReady(appointment);
        assertProviderOwnsAppointment(appointment);

        Long doctorId = appointment.getDoctorId();
        String patientEmail = appointment.getPatientEmail();
        String consultationType = normalizeConsultationType(appointment.getAppointmentType());
        LocalDateTime scheduledAt = buildScheduledAt(appointment);
        String roomName = buildRoomName(request.getAppointmentId(), doctorId);
        String joinUrl = buildJoinUrl(roomName);

        TelemedicineSession session = TelemedicineSession.builder()
                .appointmentId(request.getAppointmentId())
                .doctorId(doctorId)
                .patientId(appointment.getPatientId())
                .queueEntryId(null)
                .patientEmail(patientEmail)
                .consultationType(consultationType)
                .meetingProvider("JITSI")
                .roomName(roomName)
                .doctorJoinUrl(joinUrl)
                .patientJoinUrl(joinUrl)
                .status(SessionStatus.SCHEDULED)
                .scheduledAt(scheduledAt)
                .notes("Session created from confirmed paid appointment")
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
            Long doctorId = providerDoctorResolver.resolveDoctorId(SecurityUtils.requireCurrentUserEmail())
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
        if (session.getScheduledAt() != null && LocalDateTime.now().isBefore(session.getScheduledAt())) {
            throw new BadRequestException("Session can only be started at or after the scheduled time");
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
            Long mappedDoctorId = providerDoctorResolver.resolveDoctorId(currentUserEmail).orElse(null);
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
        Long mappedDoctorId = providerDoctorResolver.resolveDoctorId(SecurityUtils.requireCurrentUserEmail())
                .orElseThrow(() -> new ForbiddenException("No doctor profile mapped for this provider account"));
        if (!mappedDoctorId.equals(session.getDoctorId())) {
            throw new ForbiddenException("You cannot manage this telemedicine session");
        }
    }

    private void assertProviderOwnsAppointment(AppointmentResponse appointment) {
        if (SecurityUtils.hasRole("ADMIN")) {
            return;
        }
        if (!SecurityUtils.hasRole("PROVIDER")) {
            throw new ForbiddenException("Provider or admin role required");
        }
        Long mappedDoctorId = providerDoctorResolver.resolveDoctorId(SecurityUtils.requireCurrentUserEmail())
                .orElseThrow(() -> new ForbiddenException("No doctor profile mapped for this provider account"));
        if (!mappedDoctorId.equals(appointment.getDoctorId())) {
            throw new ForbiddenException("Provider account cannot create a session for another doctor's appointment");
        }
    }

    private void assertAppointmentIsTelemedicineReady(AppointmentResponse appointment) {
        if (!"CONFIRMED".equalsIgnoreCase(appointment.getStatus())) {
            throw new BadRequestException("Only confirmed appointments can create telemedicine sessions");
        }
        if (!"PAID".equalsIgnoreCase(appointment.getPaymentStatus())) {
            throw new BadRequestException("Only paid appointments can create telemedicine sessions");
        }
        if (!isTelemedicineAppointment(appointment.getAppointmentType())) {
            throw new BadRequestException("Only telemedicine appointments can create telemedicine sessions");
        }
    }

    private boolean isTelemedicineAppointment(String consultationType) {
        if (consultationType == null) {
            return false;
        }
        String normalized = consultationType.trim().toUpperCase(Locale.ROOT);
        return normalized.contains("VIDEO")
                || normalized.contains("TELE")
                || normalized.contains("ONLINE");
    }

    private String normalizeConsultationType(String consultationType) {
        if (consultationType == null || consultationType.isBlank()) {
            return "VIDEO_CONSULTATION";
        }
        return consultationType.trim().toUpperCase(Locale.ROOT);
    }

    private LocalDateTime buildScheduledAt(AppointmentResponse appointment) {
        if (appointment.getAppointmentDate() == null || appointment.getStartTime() == null) {
            return LocalDateTime.now();
        }
        return appointment.getAppointmentDate().atTime(appointment.getStartTime());
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
