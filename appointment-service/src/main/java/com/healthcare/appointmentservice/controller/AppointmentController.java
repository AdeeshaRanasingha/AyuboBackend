package com.healthcare.appointmentservice.controller;

import com.healthcare.appointmentservice.dto.ApiResponse;
import com.healthcare.appointmentservice.dto.AppointmentCreateRequest;
import com.healthcare.appointmentservice.dto.AppointmentQueueItemResponse;
import com.healthcare.appointmentservice.dto.AppointmentResponse;
import com.healthcare.appointmentservice.dto.AppointmentUpdateRequest;
import com.healthcare.appointmentservice.dto.StatusUpdateRequest;
import com.healthcare.appointmentservice.dto.SlotStatusResponse;
import com.healthcare.appointmentservice.client.AuthProviderDirectoryClient;
import com.healthcare.appointmentservice.service.AppointmentService;
import com.healthcare.appointmentservice.dto.PaymentStatusUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AuthProviderDirectoryClient authProviderDirectoryClient;

    /**
     * Proxy endpoint for the frontend doctor directory dropdowns.
     * Frontend calls /api/appointments/providers via the API gateway.
     */
    @GetMapping("/providers")
    public ApiResponse<List<Map<String, Object>>> getProviderDirectory() {
        return ApiResponse.<List<Map<String, Object>>>builder()
                .success(true)
                .message("Provider directory fetched successfully")
                .data(authProviderDirectoryClient.fetchProviderDirectory())
                .build();
    }

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
    public ApiResponse<List<SlotStatusResponse>> getAvailableSlots(
            @PathVariable("doctorId") Long doctorId,
            @RequestParam("date") String date,
            @RequestParam(name = "month", required = false) Boolean month,
            @RequestParam(name = "scope", required = false) String scope
    ) {
        boolean forCurrentMonth = Boolean.TRUE.equals(month)
                || (scope != null && "month".equalsIgnoreCase(scope.trim()));
        return ApiResponse.<List<SlotStatusResponse>>builder()
                .success(true)
                .message("Available slots fetched successfully")
                .data(appointmentService.getAvailableSlots(doctorId, date, forCurrentMonth))
                .build();
    }

    @GetMapping("/doctor/{doctorId}")
    public ApiResponse<List<AppointmentResponse>> getAppointmentsByDoctor(@PathVariable("doctorId") Long doctorId) {
        return ApiResponse.<List<AppointmentResponse>>builder()
                .success(true)
                .message("Doctor appointments fetched successfully")
                .data(appointmentService.getAppointmentsByDoctor(doctorId))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<AppointmentResponse> getAppointmentById(@PathVariable("id") Long id) {
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Appointment fetched successfully")
                .data(appointmentService.getAppointmentById(id))
                .build();
    }

    @GetMapping("/{id}/slot-queue")
    public ApiResponse<List<AppointmentQueueItemResponse>> getSlotQueueForAppointment(@PathVariable("id") Long id) {
        return ApiResponse.<List<AppointmentQueueItemResponse>>builder()
                .success(true)
                .message("Slot queue fetched successfully")
                .data(appointmentService.getSlotQueueForAppointment(id))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<AppointmentResponse> updateAppointment(
            @PathVariable("id") Long id,
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
            @PathVariable("id") Long id,
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
    public ApiResponse<String> cancelAppointment(@PathVariable("id") Long id) {
        appointmentService.cancelAppointment(id);
        return ApiResponse.<String>builder()
                .success(true)
                .message("Appointment cancelled successfully")
                .data("Appointment with id " + id + " cancelled")
                .build();
    }

    @GetMapping("/public/{id}")
    public ApiResponse<AppointmentResponse> getAppointmentPublic(@PathVariable Long id) {
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Public appointment fetched successfully")
                .data(appointmentService.getAppointmentPublic(id))
                .build();
    }

    @PatchMapping("/{id}/payment-status")
    public ApiResponse<AppointmentResponse> updatePaymentStatus(
            @PathVariable Long id,
            @RequestBody PaymentStatusUpdateRequest request
    ) {
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Appointment payment status updated successfully")
                .data(appointmentService.updatePaymentStatus(id, request))
                .build();
    }
}
