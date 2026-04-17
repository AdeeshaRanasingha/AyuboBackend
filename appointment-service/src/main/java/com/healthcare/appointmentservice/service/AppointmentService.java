package com.healthcare.appointmentservice.service;

import com.healthcare.appointmentservice.dto.AppointmentCreateRequest;
import com.healthcare.appointmentservice.dto.AppointmentResponse;
import com.healthcare.appointmentservice.dto.AppointmentUpdateRequest;
import com.healthcare.appointmentservice.dto.PaymentStatusUpdateRequest;
import com.healthcare.appointmentservice.dto.SlotStatusResponse;
import com.healthcare.appointmentservice.dto.StatusUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AppointmentService {

    AppointmentResponse createAppointment(AppointmentCreateRequest request);

    AppointmentResponse getAppointmentById(Long id);

    AppointmentResponse getAppointmentPublic(Long id);

    List<AppointmentResponse> getMyAppointments();

    List<AppointmentResponse> getAppointmentsByDoctor(Long doctorId);

    AppointmentResponse updateAppointment(Long id, AppointmentUpdateRequest request);

    AppointmentResponse updateStatus(Long id, StatusUpdateRequest request);

    AppointmentResponse updatePaymentStatus(Long id, PaymentStatusUpdateRequest request);

    void cancelAppointment(Long id);

    AppointmentResponse uploadPrescription(Long appointmentId, MultipartFile file);

    List<SlotStatusResponse> getAvailableSlots(Long doctorId, String date, boolean forCurrentMonth);
}
