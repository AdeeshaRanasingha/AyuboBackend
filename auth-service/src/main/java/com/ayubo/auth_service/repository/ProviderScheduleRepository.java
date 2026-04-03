package com.ayubo.auth_service.repository;

import com.ayubo.auth_service.model.ProviderSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProviderScheduleRepository extends JpaRepository<ProviderSchedule, Long> {
    // Custom query to grab only this specific doctor's slots
    List<ProviderSchedule> findByProviderId(Long providerId);
}