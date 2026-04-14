package com.ayubo.telemedicine_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ConfigurationProperties(prefix = "telemedicine.security")
public class TelemedicineSecurityProperties {

    private Map<String, Long> providerDoctorIdByEmail = new HashMap<>();

    public Map<String, Long> getProviderDoctorIdByEmail() {
        return providerDoctorIdByEmail;
    }

    public void setProviderDoctorIdByEmail(Map<String, Long> providerDoctorIdByEmail) {
        this.providerDoctorIdByEmail = providerDoctorIdByEmail;
    }

    public Optional<Long> doctorIdForProviderEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(providerDoctorIdByEmail.get(email.trim().toLowerCase()));
    }
}
