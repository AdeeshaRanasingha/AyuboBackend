package com.ayubo.telemedicine_service.controller;

import com.ayubo.telemedicine_service.config.JwtUtil;
import com.ayubo.telemedicine_service.dto.ApiResponse;
import com.ayubo.telemedicine_service.dto.DevTokenRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/telemedicine/dev")
@RequiredArgsConstructor
public class DevTokenController {

    private final JwtUtil jwtUtil;

    @Value("${telemedicine.dev.allow-token-endpoint:false}")
    private boolean allowTokenEndpoint;

    @PostMapping("/issue-token")
    public ApiResponse<Map<String, String>> issueToken(@Valid @RequestBody DevTokenRequest request) {
        if (!allowTokenEndpoint) {
            throw new IllegalStateException("Dev token endpoint is disabled");
        }

        String normalizedRole = request.getRole().trim().toUpperCase(Locale.ROOT);
        String token = jwtUtil.generateToken(request.getEmail().trim(), normalizedRole);

        return ApiResponse.<Map<String, String>>builder()
                .success(true)
                .message("Dev token issued successfully")
                .data(Map.of(
                        "token", token,
                        "email", request.getEmail().trim(),
                        "role", normalizedRole
                ))
                .build();
    }
}
