package com.ayubo.auth_service.controller;

import com.ayubo.auth_service.model.FeeConfiguration;
import com.ayubo.auth_service.model.MedicalProvider;
import com.ayubo.auth_service.repository.FeeConfigurationRepository;
import com.ayubo.auth_service.repository.MedicalProviderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PublicBillingController {

    private final MedicalProviderRepository providerRepository;
    private final FeeConfigurationRepository feeConfigurationRepository;

    public PublicBillingController(
            MedicalProviderRepository providerRepository,
            FeeConfigurationRepository feeConfigurationRepository
    ) {
        this.providerRepository = providerRepository;
        this.feeConfigurationRepository = feeConfigurationRepository;
    }

    @GetMapping("/provider/{id}/billing-summary")
    public ResponseEntity<?> getProviderBillingSummary(@PathVariable Long id) {
        MedicalProvider provider = providerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medical provider not found with id: " + id));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", provider.getId());
        response.put("firstName", provider.getFirstName());
        response.put("lastName", provider.getLastName());
        response.put("fullName", (provider.getFirstName() + " " + provider.getLastName()).trim());
        response.put("email", provider.getEmail());
        response.put("phone", provider.getPhone());
        response.put("hospitalName", provider.getHospitalName());
        response.put("consultationFee", provider.getConsultationFee());
        response.put("specialty", provider.getSpecialty());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/fees")
    public ResponseEntity<?> getPublicFees() {
        FeeConfiguration config = feeConfigurationRepository.findById(1L)
                .orElse(new FeeConfiguration());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("taxPercentage", config.getTaxPercentage());
        response.put("onlineConsultationFee", config.getOnlineConsultationFee());

        Map<String, Double> hospitalFees = new LinkedHashMap<>();
        hospitalFees.put("Asiri Hospital", config.getAsiriHospitalFee());
        hospitalFees.put("Nawaloka Hospital", config.getNawalokaHospitalFee());
        hospitalFees.put("Lanka Hospitals", config.getLankaHospitalFee());
        hospitalFees.put("Durdans Hospital", config.getDurdansHospitalFee());
        hospitalFees.put("Hemas Hospital", config.getHemasHospitalFee());
        hospitalFees.put("Medihelp Hospitals", config.getMedihelpHospitalFee());
        hospitalFees.put("Ninewells Hospital", config.getNinewellsHospitalFee());
        hospitalFees.put("Kings Hospital", config.getKingsHospitalFee());

        response.put("hospitalFees", hospitalFees);

        return ResponseEntity.ok(response);
    }
}