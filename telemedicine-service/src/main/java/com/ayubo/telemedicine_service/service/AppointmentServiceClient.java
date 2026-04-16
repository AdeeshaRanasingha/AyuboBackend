package com.ayubo.telemedicine_service.service;

import com.ayubo.telemedicine_service.dto.ApiResponse;
import com.ayubo.telemedicine_service.dto.AppointmentResponse;
import com.ayubo.telemedicine_service.exception.ForbiddenException;
import com.ayubo.telemedicine_service.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.appointment.base-url:http://localhost:8082}")
    private String appointmentServiceBaseUrl;

    public AppointmentResponse getAppointmentById(Long appointmentId) {
        try {
            ResponseEntity<ApiResponse<AppointmentResponse>> response = restTemplate.exchange(
                    appointmentServiceBaseUrl + "/api/appointments/" + appointmentId,
                    HttpMethod.GET,
                    new HttpEntity<>(null, buildHeaders()),
                    new ParameterizedTypeReference<ApiResponse<AppointmentResponse>>() {}
            );
            ApiResponse<AppointmentResponse> body = response.getBody();
            return body != null ? body.getData() : null;
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Appointment not found with id: " + appointmentId);
            }
            if (ex.getStatusCode().value() == 403) {
                throw new ForbiddenException("Not allowed to access appointment " + appointmentId);
            }
            throw ex;
        }
    }

    public List<AppointmentResponse> getMyAppointments() {
        ResponseEntity<ApiResponse<List<AppointmentResponse>>> response = restTemplate.exchange(
                appointmentServiceBaseUrl + "/api/appointments/my",
                HttpMethod.GET,
                new HttpEntity<>(null, buildHeaders()),
                new ParameterizedTypeReference<ApiResponse<List<AppointmentResponse>>>() {}
        );
        ApiResponse<List<AppointmentResponse>> body = response.getBody();
        return body != null && body.getData() != null ? body.getData() : List.of();
    }

    public List<AppointmentResponse> getAppointmentsByDoctor(Long doctorId) {
        ResponseEntity<ApiResponse<List<AppointmentResponse>>> response = restTemplate.exchange(
                appointmentServiceBaseUrl + "/api/appointments/doctor/" + doctorId,
                HttpMethod.GET,
                new HttpEntity<>(null, buildHeaders()),
                new ParameterizedTypeReference<ApiResponse<List<AppointmentResponse>>>() {}
        );
        ApiResponse<List<AppointmentResponse>> body = response.getBody();
        return body != null && body.getData() != null ? body.getData() : List.of();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return headers;
        }

        HttpServletRequest currentRequest = attributes.getRequest();
        String cookieHeader = currentRequest.getHeader(HttpHeaders.COOKIE);
        if (cookieHeader != null && !cookieHeader.isBlank()) {
            headers.set(HttpHeaders.COOKIE, cookieHeader);
        }

        String authorizationHeader = currentRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }

        return headers;
    }
}
