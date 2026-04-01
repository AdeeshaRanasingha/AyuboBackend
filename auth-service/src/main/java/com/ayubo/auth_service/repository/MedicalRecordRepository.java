package com.ayubo.auth_service.repository;

import com.ayubo.auth_service.model.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    // Custom query to find all documents for a logged-in patient
    List<MedicalRecord> findByPatientIdOrderByUploadDateDesc(Long patientId);
}