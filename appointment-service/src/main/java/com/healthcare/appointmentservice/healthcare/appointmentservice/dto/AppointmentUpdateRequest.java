package com.healthcare.appointmentservice.healthcare.appointmentservice.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AppointmentUpdateRequest {

    @FutureOrPresent(message = "appointmentDate cannot be in the past")
    private LocalDate appointmentDate;

    private LocalTime startTime;

    private LocalTime endTime;

    @Size(max = 255, message = "reason must not exceed 255 characters")
    private String reason;
}
