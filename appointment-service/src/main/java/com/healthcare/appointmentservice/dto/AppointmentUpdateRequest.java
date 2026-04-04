package com.healthcare.appointmentservice.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AppointmentUpdateRequest {

    @Size(max = 50, message = "appointmentFor must not exceed 50 characters")
    private String appointmentFor;

    @Size(max = 50, message = "appointmentType must not exceed 50 characters")
    private String appointmentType;

    @Size(max = 20, message = "title must not exceed 20 characters")
    private String title;

    @Size(max = 150, message = "name must not exceed 150 characters")
    private String name;

    @Size(max = 30, message = "mobile must not exceed 30 characters")
    private String mobile;

    @Size(max = 30, message = "idType must not exceed 30 characters")
    private String idType;

    @Size(max = 100, message = "idValue must not exceed 100 characters")
    private String idValue;

    @Size(max = 255, message = "email must not exceed 255 characters")
    private String email;

    @FutureOrPresent(message = "appointmentDate cannot be in the past")
    private LocalDate appointmentDate;

    private LocalTime startTime;

    private LocalTime endTime;

    @Size(max = 255, message = "reason must not exceed 255 characters")
    private String reason;

    @Size(max = 255, message = "noteOrAddress must not exceed 255 characters")
    private String noteOrAddress;

    private Boolean noShowRefund;

    private Boolean onGoingNumber;
}
