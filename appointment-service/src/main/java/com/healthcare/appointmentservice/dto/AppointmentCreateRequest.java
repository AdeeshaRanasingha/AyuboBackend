package com.healthcare.appointmentservice.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AppointmentCreateRequest {

    @NotNull(message = "patientId is required")
    private Long patientId;

    @NotNull(message = "doctorId is required")
    private Long doctorId;

    @Size(max = 100, message = "specialty must not exceed 100 characters")
    private String specialty;

    @NotNull(message = "appointmentDate is required")
    @FutureOrPresent(message = "appointmentDate cannot be in the past")
    private LocalDate appointmentDate;

    @NotNull(message = "startTime is required")
    private LocalTime startTime;

    @NotNull(message = "endTime is required")
    private LocalTime endTime;

    @Size(max = 255, message = "reason must not exceed 255 characters")
    private String reason;
}
