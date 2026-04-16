package com.ayubo.auth_service.repository;

import com.ayubo.auth_service.model.FeeConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeeConfigurationRepository extends JpaRepository<FeeConfiguration, Long> {
}