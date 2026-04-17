package com.ayubo.queue_service.service.impl;

import com.ayubo.queue_service.dto.QueueCreateRequest;
import com.ayubo.queue_service.dto.QueueResponse;
import com.ayubo.queue_service.dto.QueueStatusUpdateRequest;
import com.ayubo.queue_service.entity.QueueEntry;
import com.ayubo.queue_service.entity.QueueStatus;
import com.ayubo.queue_service.exception.BadRequestException;
import com.ayubo.queue_service.exception.ForbiddenException;
import com.ayubo.queue_service.exception.ResourceNotFoundException;
import com.ayubo.queue_service.repository.QueueEntryRepository;
import com.ayubo.queue_service.security.SecurityUtils;
import com.ayubo.queue_service.service.ProviderDoctorResolver;
import com.ayubo.queue_service.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QueueServiceImpl implements QueueService {

    private static final EnumSet<QueueStatus> ACTIVE_QUEUE_STATUSES = EnumSet.of(
            QueueStatus.WAITING,
            QueueStatus.CALLED,
            QueueStatus.IN_CONSULTATION
    );

    private final QueueEntryRepository queueEntryRepository;
    private final ProviderDoctorResolver providerDoctorResolver;

    @Override
    public QueueResponse createQueueEntry(QueueCreateRequest request) {
        if (request.getAppointmentId() != null) {
            var existing = queueEntryRepository.findByAppointmentId(request.getAppointmentId());
            if (existing.isPresent()) {
                return mapToResponse(existing.get());
            }
        }

        assertCanCreateQueueEntry(request);
        String patientEmail = resolvePatientEmail(request);
        int nextTokenNumber = queueEntryRepository
                .findTopByDoctorIdAndQueueDateOrderByTokenNumberDesc(request.getDoctorId(), request.getQueueDate())
                .map(entry -> entry.getTokenNumber() + 1)
                .orElse(1);

        QueueEntry queueEntry = QueueEntry.builder()
                .doctorId(request.getDoctorId())
                .patientId(request.getPatientId())
                .appointmentId(request.getAppointmentId())
                .patientEmail(patientEmail)
                .patientName(request.getPatientName().trim())
                .specialty(request.getSpecialty())
                .queueDate(request.getQueueDate())
                .tokenNumber(nextTokenNumber)
                .estimatedTime(request.getEstimatedTime())
                .status(QueueStatus.WAITING)
                .notes(request.getNotes())
                .build();

        return mapToResponse(queueEntryRepository.save(queueEntry));
    }

    @Override
    public List<QueueResponse> getMyQueueEntries() {
        String email = SecurityUtils.requireCurrentUserEmail();
        return queueEntryRepository.findByPatientEmailIgnoreCaseOrderByQueueDateDescCreatedAtDesc(email)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<QueueResponse> getDoctorQueue(LocalDate date) {
        SecurityUtils.requireProviderOrAdmin();
        if (SecurityUtils.hasRole("ADMIN")) {
            throw new BadRequestException("doctor queue for admin is not supported without a doctor id filter");
        }

        Long doctorId = providerDoctorResolver.requireDoctorIdForCurrentProvider();
        List<QueueEntry> entries = date != null
                ? queueEntryRepository.findByDoctorIdAndQueueDateOrderByTokenNumberAsc(doctorId, date)
                : queueEntryRepository.findByDoctorIdOrderByQueueDateDescTokenNumberAsc(doctorId);

        return entries.stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<QueueResponse> getAllQueueEntries() {
        if (!SecurityUtils.hasRole("ADMIN")) {
            throw new ForbiddenException("Admin role required");
        }
        return queueEntryRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    public QueueResponse updateQueueStatus(Long id, QueueStatusUpdateRequest request) {
        SecurityUtils.requireProviderOrAdmin();
        QueueEntry queueEntry = queueEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found with id: " + id));

        if (!SecurityUtils.hasRole("ADMIN")) {
            Long doctorId = providerDoctorResolver.requireDoctorIdForCurrentProvider();
            if (!doctorId.equals(queueEntry.getDoctorId())) {
                throw new ForbiddenException("You cannot update another doctor's queue");
            }
        }

        QueueStatus newStatus;
        try {
            newStatus = QueueStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (Exception ex) {
            throw new BadRequestException("Invalid queue status: " + request.getStatus());
        }

        queueEntry.setStatus(newStatus);
        if (request.getNotes() != null) {
            queueEntry.setNotes(request.getNotes());
        }

        return mapToResponse(queueEntryRepository.save(queueEntry));
    }

    private QueueResponse mapToResponse(QueueEntry queueEntry) {
        long aheadCount = queueEntryRepository.countByDoctorIdAndQueueDateAndStatusInAndTokenNumberLessThan(
                queueEntry.getDoctorId(),
                queueEntry.getQueueDate(),
                ACTIVE_QUEUE_STATUSES,
                queueEntry.getTokenNumber()
        );

        return QueueResponse.builder()
                .id(queueEntry.getId())
                .doctorId(queueEntry.getDoctorId())
                .patientId(queueEntry.getPatientId())
                .appointmentId(queueEntry.getAppointmentId())
                .patientEmail(queueEntry.getPatientEmail())
                .patientName(queueEntry.getPatientName())
                .specialty(queueEntry.getSpecialty())
                .queueDate(queueEntry.getQueueDate())
                .tokenNumber(queueEntry.getTokenNumber())
                .queuePosition((int) aheadCount + 1)
                .estimatedTime(queueEntry.getEstimatedTime())
                .status(queueEntry.getStatus().name())
                .notes(queueEntry.getNotes())
                .createdAt(queueEntry.getCreatedAt())
                .updatedAt(queueEntry.getUpdatedAt())
                .build();
    }

    private void assertCanCreateQueueEntry(QueueCreateRequest request) {
        if (SecurityUtils.hasRole("PROVIDER")) {
            Long doctorId = providerDoctorResolver.requireDoctorIdForCurrentProvider();
            if (!doctorId.equals(request.getDoctorId())) {
                throw new ForbiddenException("You cannot create queue entries for another doctor");
            }
            if (request.getPatientEmail() == null || request.getPatientEmail().isBlank()) {
                throw new BadRequestException("patientEmail is required when provider creates a queue entry");
            }
        }
    }

    private String resolvePatientEmail(QueueCreateRequest request) {
        if ((SecurityUtils.hasRole("PROVIDER") || SecurityUtils.hasRole("ADMIN"))
                && request.getPatientEmail() != null
                && !request.getPatientEmail().isBlank()) {
            return request.getPatientEmail().trim();
        }
        return SecurityUtils.requireCurrentUserEmail();
    }
}
