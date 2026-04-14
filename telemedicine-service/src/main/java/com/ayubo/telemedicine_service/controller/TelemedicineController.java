package com.ayubo.telemedicine_service.controller;

import com.ayubo.telemedicine_service.dto.ApiResponse;
import com.ayubo.telemedicine_service.dto.SessionCreateRequest;
import com.ayubo.telemedicine_service.dto.SessionResponse;
import com.ayubo.telemedicine_service.dto.SessionStatusUpdateRequest;
import com.ayubo.telemedicine_service.service.TelemedicineSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/telemedicine")
@RequiredArgsConstructor
public class TelemedicineController {

    private final TelemedicineSessionService telemedicineSessionService;

    @PostMapping("/sessions")
    public ApiResponse<SessionResponse> createSession(@Valid @RequestBody SessionCreateRequest request) {
        return ApiResponse.<SessionResponse>builder()
                .success(true)
                .message("Telemedicine session created successfully")
                .data(telemedicineSessionService.createSession(request))
                .build();
    }

    @GetMapping("/sessions/{id}")
    public ApiResponse<SessionResponse> getSessionById(@PathVariable Long id) {
        return ApiResponse.<SessionResponse>builder()
                .success(true)
                .message("Telemedicine session fetched successfully")
                .data(telemedicineSessionService.getSessionById(id))
                .build();
    }

    @GetMapping("/appointment/{appointmentId}")
    public ApiResponse<SessionResponse> getSessionByAppointmentId(@PathVariable Long appointmentId) {
        return ApiResponse.<SessionResponse>builder()
                .success(true)
                .message("Telemedicine session fetched successfully")
                .data(telemedicineSessionService.getSessionByAppointmentId(appointmentId))
                .build();
    }

    @GetMapping("/my")
    public ApiResponse<List<SessionResponse>> getMySessions() {
        return ApiResponse.<List<SessionResponse>>builder()
                .success(true)
                .message("Telemedicine sessions fetched successfully")
                .data(telemedicineSessionService.getMySessions())
                .build();
    }

    @PatchMapping("/sessions/{id}/start")
    public ApiResponse<SessionResponse> startSession(
            @PathVariable Long id,
            @Valid @RequestBody SessionStatusUpdateRequest request) {
        return ApiResponse.<SessionResponse>builder()
                .success(true)
                .message("Telemedicine session started successfully")
                .data(telemedicineSessionService.startSession(id, request))
                .build();
    }

    @PatchMapping("/sessions/{id}/end")
    public ApiResponse<SessionResponse> endSession(
            @PathVariable Long id,
            @Valid @RequestBody SessionStatusUpdateRequest request) {
        return ApiResponse.<SessionResponse>builder()
                .success(true)
                .message("Telemedicine session ended successfully")
                .data(telemedicineSessionService.endSession(id, request))
                .build();
    }
}
