package com.healthcare.appointmentservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthScheduleSlotRow {
    @JsonProperty("startTime")
    private String startTime;
    
    @JsonProperty("maxPatients")
    private Integer maxPatients;

    public String startTime() {
        return startTime;
    }

    public Integer maxPatients() {
        return maxPatients;
    }
}
