package com.healthcare.appointmentservice.healthcare.appointmentservice.service.impl;

import com.healthcare.appointmentservice.healthcare.appointmentservice.client.NotificationServiceClient;
import com.healthcare.appointmentservice.healthcare.appointmentservice.dto.AppointmentCreateRequest;
import com.healthcare.appointmentservice.healthcare.appointmentservice.dto.AppointmentResponse;
import com.healthcare.appointmentservice.healthcare.appointmentservice.dto.AppointmentUpdateRequest;
import com.healthcare.appointmentservice.healthcare.appointmentservice.dto.NotificationRequest;
import com.healthcare.appointmentservice.healthcare.appointmentservice.dto.StatusUpdateRequest;
import com.healthcare.appointmentservice.healthcare.appointmentservice.entity.Appointment;
import com.healthcare.appointmentservice.healthcare.appointmentservice.entity.AppointmentStatus;
import com.healthcare.appointmentservice.healthcare.appointmentservice.exception.BadRequestException;
import com.healthcare.appointmentservice.healthcare.appointmentservice.exception.ResourceNotFoundException;
import com.healthcare.appointmentservice.healthcare.appointmentservice.repository.AppointmentRepository;
import com.healthcare.appointmentservice.healthcare.appointmentservice.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final NotificationServiceClient notificationServiceClient;

    @Override
    public AppointmentResponse createAppointment(AppointmentCreateRequest request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());

        appointmentRepository.findByDoctorIdAndAppointmentDateAndStartTime(
                request.getDoctorId(),
                request.getAppointmentDate(),
                request.getStartTime()
        ).ifPresent(existing -> {
            throw new BadRequestException("This slot is already booked for the selected doctor");
        });

        Appointment appointment = Appointment.builder()
                .appointmentNumber(generateAppointmentNumber())
                .patientId(request.getPatientId())
                .doctorId(request.getDoctorId())
                .specialty(request.getSpecialty())
                .appointmentDate(request.getAppointmentDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .reason(request.getReason())
                .status(AppointmentStatus.PENDING_PAYMENT)
                .paymentStatus("PENDING")
                .meetingLink(null)
                .notes("Appointment created successfully")
                .cancelReason(null)
                .rescheduleCount(0)
                .build();

        Appointment saved = appointmentRepository.save(appointment);

        sendAppointmentCreatedNotification(saved);

        return mapToResponse(saved);
    }

    @Override
    public AppointmentResponse getAppointmentById(Long id) {
        Appointment appointment = findAppointmentById(id);
        return mapToResponse(appointment);
    }

    @Override
    public List<AppointmentResponse> getAllAppointments() {
        return appointmentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<AppointmentResponse> getAppointmentsByPatient(Long patientId) {
        return appointmentRepository.findByPatientId(patientId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<AppointmentResponse> getAppointmentsByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public AppointmentResponse updateAppointment(Long id, AppointmentUpdateRequest request) {
        Appointment appointment = findAppointmentById(id);

        if (appointment.getStatus() == AppointmentStatus.CANCELLED ||
                appointment.getStatus() == AppointmentStatus.COMPLETED ||
                appointment.getStatus() == AppointmentStatus.REJECTED) {
            throw new BadRequestException("This appointment cannot be updated in its current status");
        }

        LocalDate newDate = request.getAppointmentDate() != null ? request.getAppointmentDate() : appointment.getAppointmentDate();
        LocalTime newStartTime = request.getStartTime() != null ? request.getStartTime() : appointment.getStartTime();
        LocalTime newEndTime = request.getEndTime() != null ? request.getEndTime() : appointment.getEndTime();

        validateTimeRange(newStartTime, newEndTime);

        appointmentRepository.findByDoctorIdAndAppointmentDateAndStartTime(
                appointment.getDoctorId(),
                newDate,
                newStartTime
        ).ifPresent(existing -> {
            if (!existing.getId().equals(appointment.getId())) {
                throw new BadRequestException("Requested new slot is already booked");
            }
        });

        appointment.setAppointmentDate(newDate);
        appointment.setStartTime(newStartTime);
        appointment.setEndTime(newEndTime);

        if (request.getReason() != null) {
            appointment.setReason(request.getReason());
        }

        appointment.setStatus(AppointmentStatus.RESCHEDULED);
        appointment.setRescheduleCount(appointment.getRescheduleCount() + 1);
        appointment.setNotes("Appointment rescheduled");

        Appointment updated = appointmentRepository.save(appointment);

        sendSimpleNotification(
                "Appointment Rescheduled",
                "Your appointment " + updated.getAppointmentNumber() + " has been rescheduled to " +
                        updated.getAppointmentDate() + " " + updated.getStartTime()
        );

        return mapToResponse(updated);
    }

    @Override
    public AppointmentResponse updateStatus(Long id, StatusUpdateRequest request) {
        Appointment appointment = findAppointmentById(id);

        AppointmentStatus newStatus;
        try {
            newStatus = AppointmentStatus.valueOf(request.getStatus().toUpperCase());
        } catch (Exception ex) {
            throw new BadRequestException("Invalid appointment status: " + request.getStatus());
        }

        validateStatusTransition(appointment.getStatus(), newStatus);

        appointment.setStatus(newStatus);
        appointment.setNotes(request.getNotes());

        if (newStatus == AppointmentStatus.CONFIRMED) {
            appointment.setPaymentStatus("PAID");
        }

        Appointment updated = appointmentRepository.save(appointment);

        sendSimpleNotification(
                "Appointment Status Updated",
                "Appointment " + updated.getAppointmentNumber() + " status changed to " + updated.getStatus()
        );

        return mapToResponse(updated);
    }

    @Override
    public void cancelAppointment(Long id, String cancelReason) {
        Appointment appointment = findAppointmentById(id);

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Completed appointment cannot be cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason(cancelReason != null ? cancelReason : "Cancelled by user");
        appointment.setNotes("Appointment cancelled");

        appointmentRepository.save(appointment);

        sendSimpleNotification(
                "Appointment Cancelled",
                "Appointment " + appointment.getAppointmentNumber() + " has been cancelled"
        );
    }

    @Override
    public List<String> getAvailableSlots(Long doctorId, String date) {
        LocalDate appointmentDate = LocalDate.parse(date);

        List<Appointment> bookedAppointments =
                appointmentRepository.findByDoctorIdAndAppointmentDate(doctorId, appointmentDate);

        List<String> allSlots = new ArrayList<>();
        allSlots.add("09:00");
        allSlots.add("10:00");
        allSlots.add("11:00");
        allSlots.add("12:00");
        allSlots.add("14:00");
        allSlots.add("15:00");
        allSlots.add("16:00");

        List<String> bookedSlots = bookedAppointments.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED && a.getStatus() != AppointmentStatus.REJECTED)
                .map(a -> a.getStartTime().toString().substring(0, 5))
                .toList();

        return allSlots.stream()
                .filter(slot -> !bookedSlots.contains(slot))
                .toList();
    }

    private Appointment findAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("endTime must be after startTime");
        }
    }

    private void validateStatusTransition(AppointmentStatus currentStatus, AppointmentStatus newStatus) {
        if (currentStatus == AppointmentStatus.CANCELLED && newStatus == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Cancelled appointment cannot be completed");
        }

        if (currentStatus == AppointmentStatus.COMPLETED && newStatus != AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Completed appointment status cannot be changed");
        }
    }

    private String generateAppointmentNumber() {
        return "APT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private AppointmentResponse mapToResponse(Appointment appointment) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .appointmentNumber(appointment.getAppointmentNumber())
                .patientId(appointment.getPatientId())
                .doctorId(appointment.getDoctorId())
                .specialty(appointment.getSpecialty())
                .appointmentDate(appointment.getAppointmentDate())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .reason(appointment.getReason())
                .status(appointment.getStatus().name())
                .paymentStatus(appointment.getPaymentStatus())
                .meetingLink(appointment.getMeetingLink())
                .notes(appointment.getNotes())
                .cancelReason(appointment.getCancelReason())
                .rescheduleCount(appointment.getRescheduleCount())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }

    private void sendAppointmentCreatedNotification(Appointment appointment) {
        NotificationRequest request = NotificationRequest.builder()
                .recipientEmail("patient" + appointment.getPatientId() + "@mail.com")
                .recipientPhone("+940000000" + appointment.getPatientId())
                .subject("Appointment Created")
                .message("Your appointment " + appointment.getAppointmentNumber() +
                        " has been created for " + appointment.getAppointmentDate() +
                        " at " + appointment.getStartTime())
                .build();

        notificationServiceClient.sendNotification(request);
    }

    private void sendSimpleNotification(String subject, String message) {
        NotificationRequest request = NotificationRequest.builder()
                .recipientEmail("demo@mail.com")
                .recipientPhone("+94000000000")
                .subject(subject)
                .message(message)
                .build();

        notificationServiceClient.sendNotification(request);
    }
}