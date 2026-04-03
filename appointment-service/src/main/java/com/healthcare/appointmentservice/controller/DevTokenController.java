package com.healthcare.appointmentservice.controller;

import com.healthcare.appointmentservice.config.JwtUtil;
import com.healthcare.appointmentservice.dto.ApiResponse;
import com.healthcare.appointmentservice.dto.DevTokenRequest;
import com.healthcare.appointmentservice.exception.BadRequestException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Local Postman/testing only. Set appointment.dev.allow-token-endpoint=true in application.yml.
 * Disable in any shared or production environment.
 */
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "appointment.dev.allow-token-endpoint", havingValue = "true")
public class DevTokenController {

    private final JwtUtil jwtUtil;

    @PostMapping("/issue-token")
    public ApiResponse<Map<String, String>> issueToken(@Valid @RequestBody DevTokenRequest request) {
        String role = request.getRole().toUpperCase();
        if (!"PATIENT".equals(role) && !"PROVIDER".equals(role)) {
            throw new BadRequestException("role must be PATIENT or PROVIDER");
        }
        String token = jwtUtil.generateToken(request.getEmail().trim(), role);
        return ApiResponse.<Map<String, String>>builder()
                .success(true)
                .message("Use Authorization: Bearer <token> in Postman")
                .data(Map.of("token", token))
                .build();
    }
}
