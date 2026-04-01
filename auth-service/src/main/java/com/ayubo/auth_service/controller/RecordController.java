package com.ayubo.auth_service.controller;

import com.ayubo.auth_service.model.MedicalRecord;
import com.ayubo.auth_service.model.Patient;
import com.ayubo.auth_service.repository.MedicalRecordRepository;
import com.ayubo.auth_service.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/patient")
public class RecordController {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private MedicalRecordRepository recordRepository;

    // --- GET ALL RECORDS FOR THE LOGGED IN PATIENT ---
    @GetMapping("/records")
    public ResponseEntity<?> getMyRecords(Authentication authentication) {
        String email = authentication.getName();
        Optional<Patient> optionalPatient = patientRepository.findByEmail(email);

        if (optionalPatient.isPresent()) {
            List<MedicalRecord> records = recordRepository.findByPatientIdOrderByUploadDateDesc(optionalPatient.get().getId());
            return ResponseEntity.ok(records);
        }
        return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));
    }

    // --- UPLOAD A NEW RECORD ---
    @PostMapping("/records")
    public ResponseEntity<?> uploadRecord(@RequestBody MedicalRecord request, Authentication authentication) {
        String email = authentication.getName();
        Optional<Patient> optionalPatient = patientRepository.findByEmail(email);

        if (optionalPatient.isPresent()) {
            Patient patient = optionalPatient.get();

            // Build and save the new record
            MedicalRecord newRecord = new MedicalRecord();
            newRecord.setDocumentName(request.getDocumentName());
            newRecord.setDocumentType(request.getDocumentType());
            newRecord.setFileData(request.getFileData());
            newRecord.setUploadDate(LocalDateTime.now());
            newRecord.setPatient(patient); // Link it to the user!

            recordRepository.save(newRecord);
            return ResponseEntity.ok(Map.of("message", "Document uploaded successfully!"));
        }
        return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));
    }
}