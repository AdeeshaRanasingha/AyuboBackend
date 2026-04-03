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
        Optional<MedicalProvider> provider = providerRepository.findByEmail(email);

        if (provider.isPresent()) {
            ProviderSchedule newSlot = new ProviderSchedule();
            newSlot.setProvider(provider.get());
            newSlot.setDate(request.getDate());
            newSlot.setStartTime(request.getStartTime());
            newSlot.setEndTime(request.getEndTime());
            newSlot.setMaxPatients(request.getMaxPatients());

            scheduleRepository.save(newSlot);

            // We return the saved slot so React immediately knows its official Database ID!
            return ResponseEntity.ok(newSlot);
        }
        return ResponseEntity.status(403).body(Map.of("error", "Provider not found"));
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
}