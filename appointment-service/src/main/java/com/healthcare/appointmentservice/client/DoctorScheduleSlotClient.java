package com.healthcare.appointmentservice.client;

import com.healthcare.appointmentservice.client.dto.AuthScheduleSlotRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class DoctorScheduleSlotClient {

    private static final Logger log = LoggerFactory.getLogger(DoctorScheduleSlotClient.class);

    private static final ParameterizedTypeReference<List<AuthScheduleSlotRow>> SLOT_LIST =
            new ParameterizedTypeReference<>() {};

    private final RestClient authRestClient;

    public DoctorScheduleSlotClient(@Qualifier("authRestClient") RestClient authRestClient) {
        this.authRestClient = authRestClient;
    }

    /**
     * Loads rows from auth-service {@code doctor_schedule_slots} for channeling availability.
     */
    public List<AuthScheduleSlotRow> fetchScheduledSlots(long doctorId, String isoDate) {
        try {
            List<AuthScheduleSlotRow> body = authRestClient.get()
                    .uri("/api/schedule/doctor/{doctorId}/public-slots?date={date}", doctorId, isoDate)
                    .retrieve()
                    .body(SLOT_LIST);
            return body != null ? body : List.of();
        } catch (RestClientException ex) {
            log.warn("Could not load doctor_schedule_slots for doctor {} on {}: {}", doctorId, isoDate, ex.getMessage());
            return List.of();
        }
    }
}
