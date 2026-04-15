package com.ayubo.queue_service.service;

import com.ayubo.queue_service.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProviderDoctorResolver {

    private final RestTemplate restTemplate;

    @Value("${services.auth.base-url:http://localhost:8085}")
    private String authServiceBaseUrl;

    public Long requireDoctorIdForCurrentProvider() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                throw new ForbiddenException("Unable to resolve provider profile");
            }

            HttpServletRequest currentRequest = attributes.getRequest();
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));

            String cookieHeader = currentRequest.getHeader(HttpHeaders.COOKIE);
            if (cookieHeader != null && !cookieHeader.isBlank()) {
                headers.set(HttpHeaders.COOKIE, cookieHeader);
            }

            String authorizationHeader = currentRequest.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
            }

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    authServiceBaseUrl + "/api/provider/profile",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {}
            );

            Object idValue = response.getBody() != null ? response.getBody().get("id") : null;
            if (idValue instanceof Number) {
                return ((Number) idValue).longValue();
            }
            if (idValue instanceof String && !((String) idValue).isBlank()) {
                return Long.parseLong(((String) idValue).trim());
            }
        } catch (Exception ignored) {
            throw new ForbiddenException("No doctor profile mapped for this provider account");
        }

        throw new ForbiddenException("No doctor profile mapped for this provider account");
    }
}
