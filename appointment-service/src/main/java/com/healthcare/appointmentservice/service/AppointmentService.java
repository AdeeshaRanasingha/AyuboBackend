package com.healthcare.appointmentservice.service;

import com.healthcare.appointmentservice.dto.AppointmentCreateRequest;
import com.healthcare.appointmentservice.dto.AppointmentResponse;
import com.healthcare.appointmentservice.dto.AppointmentUpdateRequest;
import com.healthcare.appointmentservice.dto.StatusUpdateRequest;

import java.util.List;

public interface AppointmentService {

    AppointmentResponse createAppointment(AppointmentCreateRequest request);

    AppointmentResponse getAppointmentById(Long id);

    List<AppointmentResponse> getMyAppointments();

    List<AppointmentResponse> getAppointmentsByDoctor(Long doctorId);

    AppointmentResponse updateAppointment(Long id, AppointmentUpdateRequest request);

    AppointmentResponse updateStatus(Long id, StatusUpdateRequest request);

    void cancelAppointment(Long id, String cancelReason);

    List<String> getAvailableSlots(Long doctorId, String date, boolean forCurrentMonth);
    // Add this to AppointmentService.java
    AppointmentResponse uploadPrescription(Long appointmentId, org.springframework.web.multipart.MultipartFile file);
}
