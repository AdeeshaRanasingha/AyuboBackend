package com.ayubo.auth_service.controller;

import com.ayubo.auth_service.model.MedicalProvider;
import com.ayubo.auth_service.repository.MedicalProviderRepository;
import com.ayubo.auth_service.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private MedicalProviderRepository providerRepository;

    @Autowired
    private PatientRepository patientRepository;

    // 1. GET DASHBOARD STATS & LISTS
    @GetMapping("/dashboard-data")
    public ResponseEntity<?> getDashboardData() {
        long totalPatients = patientRepository.count();
        List<MedicalProvider> allProviders = providerRepository.findAll();

        Map<String, Object> response = new HashMap<>();
        response.put("totalPatients", totalPatients);
        response.put("providers", allProviders); // React will filter pending vs approved

        return ResponseEntity.ok(response);
    }

    // 2. APPROVE A DOCTOR
    @PutMapping("/provider/{id}/approve")
    public ResponseEntity<?> approveProvider(@PathVariable Long id) {
        Optional<MedicalProvider> optionalProvider = providerRepository.findById(id);
        if (optionalProvider.isPresent()) {
            MedicalProvider provider = optionalProvider.get();
            provider.setIsApproved(true);
            providerRepository.save(provider);
            return ResponseEntity.ok(Map.of("message", "Doctor approved successfully!"));
        }
        return ResponseEntity.status(404).body(Map.of("error", "Doctor not found."));
    }

    // 3. SET DOCTOR PRICING
    @PutMapping("/provider/{id}/price")
    public ResponseEntity<?> setProviderPrice(@PathVariable Long id, @RequestBody Map<String, Double> request) {
        Optional<MedicalProvider> optionalProvider = providerRepository.findById(id);
        if (optionalProvider.isPresent()) {
            MedicalProvider provider = optionalProvider.get();
            provider.setConsultationFee(request.get("fee"));
            providerRepository.save(provider);
            return ResponseEntity.ok(Map.of("message", "Pricing updated successfully!"));
        }
        return ResponseEntity.status(404).body(Map.of("error", "Doctor not found."));
    }
}