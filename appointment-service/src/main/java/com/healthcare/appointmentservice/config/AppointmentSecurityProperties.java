package com.healthcare.appointmentservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Data
@ConfigurationProperties(prefix = "appointment.security")
public class AppointmentSecurityProperties {

    /**
     * Maps provider login email (from JWT subject) to doctorId used in appointments.
     * Without an entry, a PROVIDER cannot list or modify another doctor's schedule.
     */
    private Map<String, Long> providerDoctorIdByEmail = new LinkedHashMap<>();

    public Optional<Long> doctorIdForProviderEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        for (Map.Entry<String, Long> e : providerDoctorIdByEmail.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(email)) {
                return Optional.of(e.getValue());
            }
        }
        return Optional.empty();
    }
}
