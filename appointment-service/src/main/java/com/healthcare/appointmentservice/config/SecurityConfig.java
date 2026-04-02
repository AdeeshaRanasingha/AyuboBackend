package com.healthcare.appointmentservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.appointmentservice.dto.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final boolean devTokenEndpointEnabled;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Value("${appointment.dev.allow-token-endpoint:false}") boolean devTokenEndpointEnabled) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.devTokenEndpointEnabled = devTokenEndpointEnabled;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**"
                    ).permitAll();
                    if (devTokenEndpointEnabled) {
                        auth.requestMatchers("/api/dev/issue-token").permitAll();
                    }
                    auth.requestMatchers("/api/appointments/**").authenticated();
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> writeJson(response, 401, "Authentication required"))
                        .accessDeniedHandler((request, response, accessDeniedException) -> writeJson(response, 403, accessDeniedException.getMessage()))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeJson(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResponse<Object> body = ApiResponse.builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
    }
}
