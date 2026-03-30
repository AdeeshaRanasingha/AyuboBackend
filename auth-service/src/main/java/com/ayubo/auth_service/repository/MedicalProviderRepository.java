package com.ayubo.auth_service.repository;

import com.ayubo.auth_service.model.MedicalProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicalProviderRepository extends JpaRepository<MedicalProvider, Long> {
    Optional<MedicalProvider> findByEmail(String email);
}