package com.ayubo.auth_service.controller;

import com.ayubo.auth_service.model.*;
import com.ayubo.auth_service.repository.MedicalProviderRepository;
import com.ayubo.auth_service.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ProfileController {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private MedicalProviderRepository providerRepository;

    // --- FETCH DOCTOR PROFILE ---
    @GetMapping("/provider/profile")
    public ResponseEntity<?> getProviderProfile(Authentication authentication) {
        String email = authentication.getName();

        // Ask the specific Provider table, not the generic User table!
        Optional<MedicalProvider> optionalProvider = providerRepository.findByEmail(email);

        if (optionalProvider.isPresent()) {
            MedicalProvider provider = optionalProvider.get();

            Map<String, Object> profileData = new HashMap<>();
            profileData.put("id", provider.getId());
            profileData.put("firstName", provider.getFirstName());
            profileData.put("lastName", provider.getLastName());
            profileData.put("email", provider.getEmail());
            profileData.put("phone", provider.getPhone());
            profileData.put("specialty", provider.getSpecialty());
            profileData.put("medicalLicenseNumber", provider.getMedicalLicenseNumber());
            profileData.put("hospitalName", provider.getHospitalName());
            profileData.put("bio", provider.getBio());
            profileData.put("profileImage", provider.getProfileImage());

            return ResponseEntity.ok(profileData);
        }

        // Upgraded error message so we know exactly what went wrong
        return ResponseEntity.status(403).body("{\"error\": \"Database Error: Could not find you in the Doctor directory.\"}");
    }

    // --- FETCH PATIENT PROFILE ---
    @GetMapping("/patient/profile")
    public ResponseEntity<?> getPatientProfile(Authentication authentication) {
        String email = authentication.getName();

        // Ask the specific Patient table!
        Optional<Patient> optionalPatient = patientRepository.findByEmail(email);

        if (optionalPatient.isPresent()) {
            Patient patient = optionalPatient.get();

            // Build a secure JSON object without the password
            Map<String, Object> profileData = new HashMap<>();
            profileData.put("id", patient.getId());
            profileData.put("firstName", patient.getFirstName());
            profileData.put("lastName", patient.getLastName());
            profileData.put("email", patient.getEmail());
            profileData.put("phone", patient.getPhone());
            profileData.put("dateOfBirth", patient.getDateOfBirth());
            profileData.put("bloodGroup", patient.getBloodGroup());
            profileData.put("profileImage", patient.getProfileImage());

            return ResponseEntity.ok(profileData);
        }

        return ResponseEntity.status(403).body("{\"error\": \"Database Error: Could not find you in the Patient directory.\"}");
    }

    // --- UPDATE DOCTOR PROFILE ---
    @PutMapping("/provider/profile")
    public ResponseEntity<?> updateProviderProfile(@RequestBody ProviderProfileRequest request, Authentication authentication) {
        String email = authentication.getName();
        Optional<MedicalProvider> optionalProvider = providerRepository.findByEmail(email);

        if (optionalProvider.isPresent()) {
            MedicalProvider provider = optionalProvider.get();

            provider.setFirstName(request.getFirstName());
            provider.setLastName(request.getLastName());
            provider.setPhone(request.getPhone());
            provider.setSpecialty(request.getSpecialty());
            provider.setHospitalName(request.getHospitalName());
            provider.setBio(request.getBio());

            if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
                provider.setProfileImage(request.getProfileImage());
            }

            providerRepository.save(provider);
            return ResponseEntity.ok("{\"message\": \"Professional profile updated successfully!\"}");
        }

        return ResponseEntity.status(403).body("{\"error\": \"Database Error: Could not save data to the Doctor directory.\"}");
    }

    @GetMapping("/provider/directory")
    public ResponseEntity<?> getProviderDirectory() {
        List<Map<String, Object>> directory = new ArrayList<>();

        for (MedicalProvider provider : providerRepository.findAll()) {
            Map<String, Object> doctor = new LinkedHashMap<>();
            doctor.put("id", provider.getId());
            doctor.put("firstName", provider.getFirstName());
            doctor.put("lastName", provider.getLastName());
            doctor.put("email", provider.getEmail());
            doctor.put("phone", provider.getPhone());
            doctor.put("specialty", provider.getSpecialty());
            doctor.put("hospitalName", provider.getHospitalName());
            doctor.put("bio", provider.getBio());
            doctor.put("profileImage", provider.getProfileImage());
            directory.add(doctor);
        }

        return ResponseEntity.ok(directory);
    }

    // --- UPDATE PATIENT PROFILE ---
    @PutMapping("/patient/profile")
    public ResponseEntity<?> updatePatientProfile(@RequestBody PatientProfileRequest request, Authentication authentication) {
        String email = authentication.getName();
        Optional<Patient> optionalPatient = patientRepository.findByEmail(email);

        if (optionalPatient.isPresent()) {
            Patient patient = optionalPatient.get();

            patient.setFirstName(request.getFirstName());
            patient.setLastName(request.getLastName());
            patient.setPhone(request.getPhone());
            patient.setDateOfBirth(request.getDateOfBirth());
            patient.setBloodGroup(request.getBloodGroup());

            if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
                patient.setProfileImage(request.getProfileImage());
            }

            patientRepository.save(patient);
            return ResponseEntity.ok("{\"message\": \"Profile updated successfully!\"}");
        }

        return ResponseEntity.status(403).body("{\"error\": \"Database Error: Could not find you in the Patient directory.\"}");
    }
}
