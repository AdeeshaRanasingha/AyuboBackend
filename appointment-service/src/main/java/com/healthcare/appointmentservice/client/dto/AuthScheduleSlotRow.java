package com.healthcare.appointmentservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthScheduleSlotRow(String startTime, Integer maxPatients) {}
