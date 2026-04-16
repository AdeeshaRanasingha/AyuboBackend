package com.ayubo.auth_service.controller;

import com.ayubo.auth_service.dto.FeeConfigurationDTO;
import com.ayubo.auth_service.model.FeeConfiguration;
import com.ayubo.auth_service.repository.FeeConfigurationRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/fees")
public class AdminFeeController {

    @Autowired
    private FeeConfigurationRepository feeRepository;

    // 1. Seed the database on startup if empty
    @PostConstruct
    public void initDefaultData() {
        if (feeRepository.count() == 0) {
            FeeConfiguration config = new FeeConfiguration();
            config.setTaxPercentage(15.0);
            config.setOnlineConsultationFee(1500.0);
            config.setAsiriHospitalFee(2000.0);
            config.setNawalokaHospitalFee(2000.0);
            config.setLankaHospitalFee(2000.0);
            config.setDurdansHospitalFee(2000.0);
            config.setHemasHospitalFee(2000.0);
            config.setMedihelpHospitalFee(2000.0);
            config.setNinewellsHospitalFee(2000.0);
            config.setKingsHospitalFee(2000.0);
            feeRepository.save(config);
        }
    }

    // 2. GET Endpoint: Map Flat Entity -> DTO Array for React
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FeeConfigurationDTO> getFees() {
        FeeConfiguration config = feeRepository.findById(1L).orElse(new FeeConfiguration());

        FeeConfigurationDTO response = new FeeConfigurationDTO();
        response.setTaxPercentage(config.getTaxPercentage());
        response.setOnlineConsultationFee(config.getOnlineConsultationFee());

        List<FeeConfigurationDTO.HospitalDTO> hospitalDTOs = new ArrayList<>();
        hospitalDTOs.add(new FeeConfigurationDTO.HospitalDTO(1L, "Asiri Hospital", config.getAsiriHospitalFee()));
        hospitalDTOs.add(new FeeConfigurationDTO.HospitalDTO(2L, "Nawaloka Hospital", config.getNawalokaHospitalFee()));
        hospitalDTOs.add(new FeeConfigurationDTO.HospitalDTO(3L, "Lanka Hospitals", config.getLankaHospitalFee()));
        hospitalDTOs.add(new FeeConfigurationDTO.HospitalDTO(4L, "Durdans Hospital", config.getDurdansHospitalFee()));
        hospitalDTOs.add(new FeeConfigurationDTO.HospitalDTO(5L, "Hemas Hospital", config.getHemasHospitalFee()));
        hospitalDTOs.add(new FeeConfigurationDTO.HospitalDTO(6L, "Medihelp Hospitals", config.getMedihelpHospitalFee()));
        hospitalDTOs.add(new FeeConfigurationDTO.HospitalDTO(7L, "Ninewells Hospital", config.getNinewellsHospitalFee()));
        hospitalDTOs.add(new FeeConfigurationDTO.HospitalDTO(8L, "Kings Hospital", config.getKingsHospitalFee()));

        response.setHospitalFees(hospitalDTOs);

        return ResponseEntity.ok(response);
    }

    // 3. PUT Endpoint: Map DTO Array -> Flat Entity for Database
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateFees(@RequestBody FeeConfigurationDTO request) {
        FeeConfiguration config = feeRepository.findById(1L).orElse(new FeeConfiguration());

        config.setTaxPercentage(request.getTaxPercentage());
        config.setOnlineConsultationFee(request.getOnlineConsultationFee());

        // Update the specific flat columns based on the ID from the React array
        for (FeeConfigurationDTO.HospitalDTO hDto : request.getHospitalFees()) {
            switch (hDto.getId().intValue()) {
                case 1: config.setAsiriHospitalFee(hDto.getFee()); break;
                case 2: config.setNawalokaHospitalFee(hDto.getFee()); break;
                case 3: config.setLankaHospitalFee(hDto.getFee()); break;
                case 4: config.setDurdansHospitalFee(hDto.getFee()); break;
                case 5: config.setHemasHospitalFee(hDto.getFee()); break;
                case 6: config.setMedihelpHospitalFee(hDto.getFee()); break;
                case 7: config.setNinewellsHospitalFee(hDto.getFee()); break;
                case 8: config.setKingsHospitalFee(hDto.getFee()); break;
            }
        }

        feeRepository.save(config);

        return ResponseEntity.ok().body(Map.of("message", "Fees updated successfully"));
    }
}