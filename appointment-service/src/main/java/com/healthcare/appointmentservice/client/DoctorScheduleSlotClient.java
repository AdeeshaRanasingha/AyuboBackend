package com.healthcare.appointmentservice.client;

import com.healthcare.appointmentservice.client.dto.AuthScheduleSlotRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;

@Component
public class DoctorScheduleSlotClient {

    private static final Logger log = LoggerFactory.getLogger(DoctorScheduleSlotClient.class);

    private static final ParameterizedTypeReference<List<AuthScheduleSlotRow>> SLOT_LIST =
            new ParameterizedTypeReference<List<AuthScheduleSlotRow>>() {};

    private final RestClient authRestClient;
    private final RestClient authFallbackRestClient;
    private final String authBaseUrl;
    private final String authFallbackBaseUrl;

    public DoctorScheduleSlotClient(
            @Qualifier("authRestClient") RestClient authRestClient,
            @Qualifier("authFallbackRestClient") RestClient authFallbackRestClient,
            @Value("${services.auth.base-url:http://localhost:8090}") String authBaseUrl,
            @Value("${services.auth.fallback-base-url:http://host.docker.internal:8090}") String authFallbackBaseUrl
    ) {
        this.authRestClient = authRestClient;
        this.authFallbackRestClient = authFallbackRestClient;
        this.authBaseUrl = authBaseUrl;
        this.authFallbackBaseUrl = authFallbackBaseUrl;
    }

    /**
     * Loads rows from auth-service {@code doctor_schedule_slots} for channeling availability.
     */
    public List<AuthScheduleSlotRow> fetchScheduledSlots(long doctorId, String isoDate) {
        try {
            return fetchScheduledSlots(authRestClient, doctorId, isoDate);
        } catch (RestClientException ex) {
            if (shouldTryFallback()) {
                try {
                    return fetchScheduledSlots(authFallbackRestClient, doctorId, isoDate);
                } catch (RestClientException fallbackEx) {
                    log.warn(
                            "Could not load doctor_schedule_slots for doctor {} on {}: {} (fallback: {})",
                            doctorId,
                            isoDate,
                            ex.getMessage(),
                            fallbackEx.getMessage()
                    );
                    return Collections.emptyList();
                }
            }
            log.warn("Could not load doctor_schedule_slots for doctor {} on {}: {}", doctorId, isoDate, ex.getMessage());
            return Collections.emptyList();
        }
    }

    private List<AuthScheduleSlotRow> fetchScheduledSlots(RestClient client, long doctorId, String isoDate) {
        List<AuthScheduleSlotRow> body = client.get()
                .uri("/api/schedule/doctor/{doctorId}/public-slots?date={date}", doctorId, isoDate)
                .retrieve()
                .body(SLOT_LIST);
        return body != null ? body : Collections.emptyList();
    }

    private boolean shouldTryFallback() {
        if (authFallbackBaseUrl == null || authFallbackBaseUrl.trim().isEmpty()) {
            return false;
        }
        if (authBaseUrl == null || authBaseUrl.trim().isEmpty()) {
            return true;
        }
        return !authBaseUrl.trim().equalsIgnoreCase(authFallbackBaseUrl.trim());
    }
}
