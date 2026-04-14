package com.ayubo.telemedicine_service.security;

import com.ayubo.telemedicine_service.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String requireCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null || auth.getName().isBlank()) {
            throw new ForbiddenException("Authentication required");
        }
        return auth.getName();
    }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        String expected = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (expected.equalsIgnoreCase(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public static void requireProviderOrAdmin() {
        if (!hasRole("PROVIDER") && !hasRole("ADMIN")) {
            throw new ForbiddenException("Provider or admin role required");
        }
    }
}
