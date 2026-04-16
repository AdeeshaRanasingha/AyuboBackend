package com.healthcare.appointmentservice.controller;

import com.healthcare.appointmentservice.dto.ApiResponse;
import com.healthcare.appointmentservice.dto.AppointmentCreateRequest;
import com.healthcare.appointmentservice.dto.AppointmentResponse;
import com.healthcare.appointmentservice.dto.AppointmentUpdateRequest;
import com.healthcare.appointmentservice.dto.StatusUpdateRequest;
import com.healthcare.appointmentservice.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Value("${app.integration.internal-token:}")
    private String internalToken;

    @PostMapping
    public ApiResponse<AppointmentResponse> createAppointment(@Valid @RequestBody AppointmentCreateRequest request) {
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Appointment created successfully")
                .data(appointmentService.createAppointment(request))
                .build();
    }

    @PostMapping(value = "/{id}/prescription", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> uploadPrescription(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        AppointmentResponse updatedAppointment = appointmentService.uploadPrescription(id, file);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Prescription uploaded successfully")
                .data(updatedAppointment)
                .build());
    }

    @GetMapping("/my")
    public ApiResponse<List<AppointmentResponse>> getMyAppointments() {
        return ApiResponse.<List<AppointmentResponse>>builder()
                .success(true)
                .message("Your appointments fetched successfully")
                .data(appointmentService.getMyAppointments())
                .build();
    }

    @GetMapping("/doctor/{doctorId}/available-slots")
    public ApiResponse<List<String>> getAvailableSlots(
            @PathVariable Long doctorId,
            @RequestParam String date,
            @RequestParam(name = "month", required = false) Boolean month,
            @RequestParam(name = "scope", required = false) String scope
    ) {
        boolean forCurrentMonth = Boolean.TRUE.equals(month)
                || (scope != null && "month".equalsIgnoreCase(scope.trim()));
        return ApiResponse.<List<String>>builder()
                .success(true)
                .message("Available slots fetched successfully")
                .data(appointmentService.getAvailableSlots(doctorId, date, forCurrentMonth))
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

    @GetMapping("/{id}")
    public ApiResponse<AppointmentResponse> getAppointmentById(@PathVariable Long id) {
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Appointment fetched successfully")
                .data(appointmentService.getAppointmentById(id))
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

    @PatchMapping("/{id}/payment-status/paid")
    public ApiResponse<AppointmentResponse> markPaymentAsPaid(
            @PathVariable Long id,
            @RequestHeader(value = "X-Internal-Token", required = false) String token
    ) {
        if (internalToken == null || internalToken.isBlank() || !internalToken.equals(token)) {
            throw new com.healthcare.appointmentservice.exception.ForbiddenException("Invalid internal token");
        }

        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Appointment payment status updated successfully")
                .data(appointmentService.markPaymentAsPaid(id))
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
}
