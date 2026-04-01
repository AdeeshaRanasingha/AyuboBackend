package com.healthcare.appointmentservice.healthcare.appointmentservice.controller;

import com.healthcare.appointmentservice.healthcare.appointmentservice.dto.ApiResponse;
import com.healthcare.appointmentservice.healthcare.appointmentservice.dto.AppointmentCreateRequest;
import com.healthcare.appointmentservice.healthcare.appointmentservice.dto.AppointmentResponse;
import com.healthcare.appointmentservice.healthcare.appointmentservice.dto.AppointmentUpdateRequest;
import com.healthcare.appointmentservice.healthcare.appointmentservice.dto.StatusUpdateRequest;
import com.healthcare.appointmentservice.healthcare.appointmentservice.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ApiResponse<AppointmentResponse> createAppointment(@Valid @RequestBody AppointmentCreateRequest request) {
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Appointment created successfully")
                .data(appointmentService.createAppointment(request))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<AppointmentResponse> getAppointmentById(@PathVariable Long id) {
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Appointment fetched successfully")
                .data(appointmentService.getAppointmentById(id))
                .build();
    }

    @GetMapping
    public ApiResponse<List<AppointmentResponse>> getAllAppointments() {
        return ApiResponse.<List<AppointmentResponse>>builder()
                .success(true)
                .message("All appointments fetched successfully")
                .data(appointmentService.getAllAppointments())
                .build();
    }

    @GetMapping("/patient/{patientId}")
    public ApiResponse<List<AppointmentResponse>> getAppointmentsByPatient(@PathVariable Long patientId) {
        return ApiResponse.<List<AppointmentResponse>>builder()
                .success(true)
                .message("Patient appointments fetched successfully")
                .data(appointmentService.getAppointmentsByPatient(patientId))
                .build();
    }

    @GetMapping("/doctor/{doctorId}")
    public ApiResponse<List<AppointmentResponse>> getAppointmentsByDoctor(@PathVariable Long doctorId) {
        return ApiResponse.<List<AppointmentResponse>>builder()
                .success(true)
                .message("Doctor appointments fetched successfully")
                .data(appointmentService.getAppointmentsByDoctor(doctorId))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<AppointmentResponse> updateAppointment(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentUpdateRequest request
    ) {
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Appointment updated successfully")
                .data(appointmentService.updateAppointment(id, request))
                .build();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AppointmentResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Appointment status updated successfully")
                .data(appointmentService.updateStatus(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> cancelAppointment(
            @PathVariable Long id,
            @RequestParam(required = false) String reason
    ) {
        appointmentService.cancelAppointment(id, reason);
        return ApiResponse.<String>builder()
                .success(true)
                .message("Appointment cancelled successfully")
                .data("Appointment with id " + id + " cancelled")
                .build();
    }

    @GetMapping("/doctor/{doctorId}/available-slots")
    public ApiResponse<List<String>> getAvailableSlots(
            @PathVariable Long doctorId,
            @RequestParam String date
    ) {
        return ApiResponse.<List<String>>builder()
                .success(true)
                .message("Available slots fetched successfully")
                .data(appointmentService.getAvailableSlots(doctorId, date))
                .build();
    }
}