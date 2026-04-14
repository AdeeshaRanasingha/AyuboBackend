package com.ayubo.telemedicine_service.repository;

import com.ayubo.telemedicine_service.entity.TelemedicineSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TelemedicineSessionRepository extends JpaRepository<TelemedicineSession, Long> {
    Optional<TelemedicineSession> findByAppointmentId(Long appointmentId);
    List<TelemedicineSession> findByPatientEmailIgnoreCaseOrderByScheduledAtDesc(String patientEmail);
    List<TelemedicineSession> findByDoctorIdOrderByScheduledAtDesc(Long doctorId);
}
