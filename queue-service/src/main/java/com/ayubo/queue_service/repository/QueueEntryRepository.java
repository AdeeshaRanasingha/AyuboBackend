package com.ayubo.queue_service.repository;

import com.ayubo.queue_service.entity.QueueEntry;
import com.ayubo.queue_service.entity.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {
    List<QueueEntry> findByPatientEmailIgnoreCaseOrderByQueueDateDescCreatedAtDesc(String patientEmail);

    List<QueueEntry> findByDoctorIdAndQueueDateOrderByTokenNumberAsc(Long doctorId, LocalDate queueDate);

    List<QueueEntry> findByDoctorIdOrderByQueueDateDescTokenNumberAsc(Long doctorId);

    Optional<QueueEntry> findTopByDoctorIdAndQueueDateOrderByTokenNumberDesc(Long doctorId, LocalDate queueDate);

    long countByDoctorIdAndQueueDateAndStatusInAndTokenNumberLessThan(
            Long doctorId,
            LocalDate queueDate,
            Collection<QueueStatus> statuses,
            Integer tokenNumber
    );
}
