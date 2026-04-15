package com.ayubo.telemedicine_service.service;

import com.ayubo.telemedicine_service.config.TelemedicineSecurityProperties;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProviderDoctorResolver {

    private final TelemedicineSecurityProperties telemedicineSecurityProperties;
    private final RestTemplate restTemplate;

    @Value("${services.auth.base-url:http://localhost:8085}")
    private String authServiceBaseUrl;

    public Optional<Long> resolveDoctorId(String providerEmail) {
        Optional<Long> configuredId = telemedicineSecurityProperties.doctorIdForProviderEmail(providerEmail);
        if (configuredId.isPresent()) {
            return configuredId;
        }

        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return Optional.empty();
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
                Number number = (Number) idValue;
                return Optional.of(number.longValue());
            }
            if (idValue instanceof String) {
                String text = (String) idValue;
                if (!text.isBlank()) {
                    return Optional.of(Long.parseLong(text.trim()));
                }
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }

        return Optional.empty();
    }
}
