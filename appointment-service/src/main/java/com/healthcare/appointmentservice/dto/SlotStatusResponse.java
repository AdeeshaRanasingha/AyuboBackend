package com.healthcare.appointmentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotStatusResponse {
    private String startTime;
    private Integer maxPatients;
    private Long availableSlots;
    private String status; // "AVAILABLE" or "SOLD OUT"
}
