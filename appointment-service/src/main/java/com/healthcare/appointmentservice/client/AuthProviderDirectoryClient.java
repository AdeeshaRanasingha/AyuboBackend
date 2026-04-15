package com.healthcare.appointmentservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves medical_provider.id from auth-service public directory (matches {@code doctorId} on appointments).
 */
@Component
public class AuthProviderDirectoryClient {

    private static final Logger log = LoggerFactory.getLogger(AuthProviderDirectoryClient.class);

    private static final ParameterizedTypeReference<List<Map<String, Object>>> DIRECTORY_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient authRestClient;
    private final RestClient authFallbackRestClient;
    private final String authBaseUrl;
    private final String authFallbackBaseUrl;

    public AuthProviderDirectoryClient(
            @Qualifier("authRestClient") RestClient authRestClient,
            @Qualifier("authFallbackRestClient") RestClient authFallbackRestClient,
            @Value("${services.auth.base-url:http://localhost:8085}") String authBaseUrl,
            @Value("${services.auth.fallback-base-url:http://host.docker.internal:8085}") String authFallbackBaseUrl
    ) {
        this.authRestClient = authRestClient;
        this.authFallbackRestClient = authFallbackRestClient;
        this.authBaseUrl = authBaseUrl;
        this.authFallbackBaseUrl = authFallbackBaseUrl;
    }

    public Optional<Long> findProviderIdByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        try {
            List<Map<String, Object>> body = fetchDirectory(authRestClient);
            if (body == null) {
                return Optional.empty();
            }
            String needle = email.trim().toLowerCase();
            for (Map<String, Object> row : body) {
                Object emailObj = row.get("email");
                if (emailObj != null && needle.equals(emailObj.toString().trim().toLowerCase())) {
                    return parseId(row.get("id"));
                }
            }
        } catch (RestClientException ex) {
            if (shouldTryFallback()) {
                try {
                    List<Map<String, Object>> fallback = fetchDirectory(authFallbackRestClient);
                    if (fallback == null) {
                        return Optional.empty();
                    }
                    String needle = email.trim().toLowerCase();
                    for (Map<String, Object> row : fallback) {
                        Object emailObj = row.get("email");
                        if (emailObj != null && needle.equals(emailObj.toString().trim().toLowerCase())) {
                            return parseId(row.get("id"));
                        }
                    }
                } catch (RestClientException fallbackEx) {
                    log.warn("Could not load /api/provider/directory: {} (fallback: {})", ex.getMessage(), fallbackEx.getMessage());
                }
            } else {
                log.warn("Could not load /api/provider/directory: {}", ex.getMessage());
            }
        }
        return Optional.empty();
    }

    private List<Map<String, Object>> fetchDirectory(RestClient client) {
        return client.get()
                .uri("/api/provider/directory")
                .retrieve()
                .body(DIRECTORY_TYPE);
    }

    private boolean shouldTryFallback() {
        if (authFallbackBaseUrl == null || authFallbackBaseUrl.isBlank()) {
            return false;
        }
        if (authBaseUrl == null || authBaseUrl.isBlank()) {
            return true;
        }
        return !authBaseUrl.trim().equalsIgnoreCase(authFallbackBaseUrl.trim());
    }

    private static Optional<Long> parseId(Object id) {
        if (id == null) {
            return Optional.empty();
        }
        if (id instanceof Number n) {
            return Optional.of(n.longValue());
        }
        try {
            return Optional.of(Long.parseLong(id.toString().trim()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
