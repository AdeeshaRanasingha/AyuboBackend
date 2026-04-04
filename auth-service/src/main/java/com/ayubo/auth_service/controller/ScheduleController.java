package com.ayubo.auth_service.controller;

import com.ayubo.auth_service.model.MedicalProvider;
import com.ayubo.auth_service.model.ProviderSchedule;
import com.ayubo.auth_service.model.ScheduleRequest;
import com.ayubo.auth_service.repository.MedicalProviderRepository;
import com.ayubo.auth_service.repository.ProviderScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    @Autowired
    private ProviderScheduleRepository scheduleRepository;

    @Autowired
    private MedicalProviderRepository providerRepository;

    // 1. GET ALL SLOTS FOR LOGGED-IN DOCTOR
    @GetMapping
    public ResponseEntity<?> getMySchedule(Authentication authentication) {
        String email = authentication.getName();
        Optional<MedicalProvider> provider = providerRepository.findByEmail(email);

        if (provider.isPresent()) {
            List<ProviderSchedule> slots = scheduleRepository.findByProviderId(provider.get().getId());
            return ResponseEntity.ok(slots);
        }
        return ResponseEntity.status(403).body(Map.of("error", "Provider not found"));
    }

    // 2. ADD A NEW SLOT
    @PostMapping
    public ResponseEntity<?> addScheduleSlot(@RequestBody ScheduleRequest request, Authentication authentication) {
        String email = authentication.getName();
        Optional<MedicalProvider> providerOpt = providerRepository.findByEmail(email);

        if (providerOpt.isPresent()) {
            MedicalProvider provider = providerOpt.get();

            // =========================================================
            // SECURITY CHECK: ONLY APPROVED DOCTORS CAN ADD SLOTS
            // =========================================================
            if (provider.getIsApproved() == null || !provider.getIsApproved()) {
                return ResponseEntity.status(403).body("Your account is pending Admin approval. You cannot add schedule slots yet.");
            }

            ProviderSchedule newSlot = new ProviderSchedule();
            newSlot.setProvider(provider);
            newSlot.setDate(request.getDate());
            newSlot.setStartTime(request.getStartTime());
            newSlot.setEndTime(request.getEndTime());
            newSlot.setMaxPatients(request.getMaxPatients());
            newSlot.setHospitalName(request.getHospitalName());

            scheduleRepository.save(newSlot);

            return ResponseEntity.ok(newSlot);
        }
        return ResponseEntity.status(403).body("Provider not found");
    }

    // 3. DELETE A SLOT
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteScheduleSlot(@PathVariable Long id) {
        if (scheduleRepository.existsById(id)) {
            scheduleRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Time slot deleted."));
        }
        return ResponseEntity.notFound().build();
    }

    // 4. UPDATE/EDIT A SLOT
    @PutMapping("/{id}")
    public ResponseEntity<?> updateScheduleSlot(
            @PathVariable Long id,
            @RequestBody ScheduleRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        Optional<MedicalProvider> provider = providerRepository.findByEmail(email);

        if (provider.isPresent()) {
            Optional<ProviderSchedule> optionalSlot = scheduleRepository.findById(id);

            if (optionalSlot.isPresent()) {
                ProviderSchedule existingSlot = optionalSlot.get();

                // Security check: Ensure the doctor actually owns this slot before updating
                if (!existingSlot.getProvider().getId().equals(provider.get().getId())) {
                    return ResponseEntity.status(403).body(Map.of("error", "Unauthorized to edit this slot."));
                }

                // Update the fields
                existingSlot.setDate(request.getDate());
                existingSlot.setStartTime(request.getStartTime());
                existingSlot.setEndTime(request.getEndTime());
                existingSlot.setMaxPatients(request.getMaxPatients());
                existingSlot.setHospitalName(request.getHospitalName());

                scheduleRepository.save(existingSlot);

                // Return the updated slot back to React
                return ResponseEntity.ok(existingSlot);
            }
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(403).body(Map.of("error", "Provider not found"));
    }
}