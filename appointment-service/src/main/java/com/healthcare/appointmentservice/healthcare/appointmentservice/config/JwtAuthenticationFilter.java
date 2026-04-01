package com.healthcare.appointmentservice.healthcare.appointmentservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;

        // 1. Look for the "ayubo_jwt" cookie in the incoming request
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("ayubo_jwt".equals(cookie.getName())) {
                    token = cookie.getValue();
                }
            }
        }

        // 2. If we found a token, let's validate it!
        if (token != null && jwtUtil.validateToken(token)) {
            String email = jwtUtil.extractEmail(token);
            String role = jwtUtil.extractRole(token);

            // 3. Create the official Spring Security "VIP Pass"
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    email, null, Collections.singletonList(authority));

            // 4. Stick the VIP Pass to the request so the Controller knows who is asking
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 5. Send the request to the next step (either the Controller, or a 403 Forbidden Error)
        filterChain.doFilter(request, response);
    }
}
