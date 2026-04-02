package com.healthcare.appointmentservice.service.impl;

import com.healthcare.appointmentservice.client.NotificationServiceClient;
import com.healthcare.appointmentservice.config.AppointmentSecurityProperties;
import com.healthcare.appointmentservice.dto.AppointmentCreateRequest;
import com.healthcare.appointmentservice.dto.AppointmentResponse;
import com.healthcare.appointmentservice.dto.AppointmentUpdateRequest;
import com.healthcare.appointmentservice.dto.NotificationRequest;
import com.healthcare.appointmentservice.dto.StatusUpdateRequest;
import com.healthcare.appointmentservice.entity.Appointment;
import com.healthcare.appointmentservice.entity.AppointmentStatus;
import com.healthcare.appointmentservice.exception.BadRequestException;
import com.healthcare.appointmentservice.exception.ForbiddenException;
import com.healthcare.appointmentservice.exception.ResourceNotFoundException;
import com.healthcare.appointmentservice.repository.AppointmentRepository;
import com.healthcare.appointmentservice.security.SecurityUtils;
import com.healthcare.appointmentservice.service.AppointmentService;
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
    private final AppointmentSecurityProperties appointmentSecurityProperties;

    @Override
    public AppointmentResponse createAppointment(AppointmentCreateRequest request) {
        String patientEmail = resolvePatientEmail(request);
        String contactEmail = normalizeContactEmail(request.getEmail(), patientEmail);

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
                .patientEmail(patientEmail)
                .appointmentFor(request.getAppointmentFor())
                .appointmentType(request.getAppointmentType())
                .patientTitle(request.getTitle())
                .patientName(request.getName())
                .contactNumber(request.getMobile())
                .identificationType(request.getIdType())
                .identificationValue(request.getIdValue())
                .contactEmail(contactEmail)
                .doctorId(request.getDoctorId())
                .specialty(request.getSpecialty())
                .appointmentDate(request.getAppointmentDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .reason(request.getReason())
                .noteOrAddress(request.getNoteOrAddress())
                .noShowRefund(Boolean.TRUE.equals(request.getNoShowRefund()))
                .onGoingNumber(Boolean.TRUE.equals(request.getOnGoingNumber()))
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
        assertCanAccessAppointment(appointment);
        return mapToResponse(appointment);
    }

    @Override
    public List<AppointmentResponse> getMyAppointments() {
        SecurityUtils.requirePatient();
        String email = SecurityUtils.requireCurrentUserEmail();
        return appointmentRepository.findByPatientEmailIgnoreCase(email)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<AppointmentResponse> getAppointmentsByDoctor(Long doctorId) {
        SecurityUtils.requireProvider();
        Long mappedDoctorId = appointmentSecurityProperties.doctorIdForProviderEmail(SecurityUtils.requireCurrentUserEmail())
                .orElseThrow(() -> new ForbiddenException("No doctor profile mapped for this provider account"));
        if (!mappedDoctorId.equals(doctorId)) {
            throw new ForbiddenException("You cannot view appointments for this doctor");
        }
        return appointmentRepository.findByDoctorId(doctorId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public AppointmentResponse updateAppointment(Long id, AppointmentUpdateRequest request) {
        Appointment appointment = findAppointmentById(id);
        assertCanModifyAppointmentAsPatientOrProvider(appointment);

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
                notificationRecipient(appointment),
                "Appointment Rescheduled",
                "Your appointment " + updated.getAppointmentNumber() + " has been rescheduled to " +
                        updated.getAppointmentDate() + " " + updated.getStartTime()
        );

        return mapToResponse(updated);
    }

    @Override
    public AppointmentResponse updateStatus(Long id, StatusUpdateRequest request) {
        SecurityUtils.requireProvider();
        Appointment appointment = findAppointmentById(id);
        Long mappedDoctorId = appointmentSecurityProperties.doctorIdForProviderEmail(SecurityUtils.requireCurrentUserEmail())
                .orElseThrow(() -> new ForbiddenException("No doctor profile mapped for this provider account"));
        if (!mappedDoctorId.equals(appointment.getDoctorId())) {
            throw new ForbiddenException("You cannot update status for this appointment");
        }

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
                notificationRecipient(appointment),
                "Appointment Status Updated",
                "Appointment " + updated.getAppointmentNumber() + " status changed to " + updated.getStatus()
        );

        return mapToResponse(updated);
    }

    @Override
    public void cancelAppointment(Long id, String cancelReason) {
        Appointment appointment = findAppointmentById(id);
        assertCanModifyAppointmentAsPatientOrProvider(appointment);

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Completed appointment cannot be cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason(cancelReason != null ? cancelReason : "Cancelled by user");
        appointment.setNotes("Appointment cancelled");

        appointmentRepository.save(appointment);

        sendSimpleNotification(
                notificationRecipient(appointment),
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

    private void assertCanAccessAppointment(Appointment appointment) {
        String email = SecurityUtils.requireCurrentUserEmail();
        if (SecurityUtils.hasRole("PATIENT") && email.equalsIgnoreCase(appointment.getPatientEmail())) {
            return;
        }
        if (SecurityUtils.hasRole("PROVIDER")) {
            Long mapped = appointmentSecurityProperties.doctorIdForProviderEmail(email).orElse(null);
            if (mapped != null && mapped.equals(appointment.getDoctorId())) {
                return;
            }
        }
        throw new ForbiddenException("Not allowed to access this appointment");
    }

    private void assertCanModifyAppointmentAsPatientOrProvider(Appointment appointment) {
        String email = SecurityUtils.requireCurrentUserEmail();
        boolean asPatient = SecurityUtils.hasRole("PATIENT") && email.equalsIgnoreCase(appointment.getPatientEmail());
        boolean asProvider = SecurityUtils.hasRole("PROVIDER")
                && appointmentSecurityProperties.doctorIdForProviderEmail(email)
                .map(d -> d.equals(appointment.getDoctorId()))
                .orElse(false);
        if (!asPatient && !asProvider) {
            throw new ForbiddenException("Not allowed to modify this appointment");
        }
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
                .patientEmail(appointment.getPatientEmail())
                .appointmentFor(appointment.getAppointmentFor())
                .appointmentType(appointment.getAppointmentType())
                .patientTitle(appointment.getPatientTitle())
                .patientName(appointment.getPatientName())
                .contactNumber(appointment.getContactNumber())
                .identificationType(appointment.getIdentificationType())
                .identificationValue(appointment.getIdentificationValue())
                .contactEmail(appointment.getContactEmail())
                .doctorId(appointment.getDoctorId())
                .specialty(appointment.getSpecialty())
                .appointmentDate(appointment.getAppointmentDate())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .reason(appointment.getReason())
                .noteOrAddress(appointment.getNoteOrAddress())
                .noShowRefund(appointment.getNoShowRefund())
                .onGoingNumber(appointment.getOnGoingNumber())
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
                .recipientEmail(notificationRecipient(appointment))
                .recipientPhone(null)
                .subject("Appointment Created")
                .message("Your appointment " + appointment.getAppointmentNumber() +
                        " has been created for " + appointment.getAppointmentDate() +
                        " at " + appointment.getStartTime())
                .build();

        notificationServiceClient.sendNotification(request);
    }

    private void sendSimpleNotification(String recipientEmail, String subject, String message) {
        NotificationRequest request = NotificationRequest.builder()
                .recipientEmail(recipientEmail)
                .recipientPhone(null)
                .subject(subject)
                .message(message)
                .build();

        notificationServiceClient.sendNotification(request);
    }

    private String normalizeContactEmail(String requestedEmail, String fallbackEmail) {
        if (requestedEmail == null || requestedEmail.isBlank()) {
            return fallbackEmail;
        }
        return requestedEmail.trim();
    }

    private String resolvePatientEmail(AppointmentCreateRequest request) {
        if (SecurityUtils.hasRole("PATIENT")) {
            return SecurityUtils.requireCurrentUserEmail();
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            return request.getEmail().trim();
        }
        return "guest-" + UUID.randomUUID().toString().substring(0, 8) + "@ayubo.local";
    }

    private String notificationRecipient(Appointment appointment) {
        if (appointment.getContactEmail() != null && !appointment.getContactEmail().isBlank()) {
            return appointment.getContactEmail();
        }
        return appointment.getPatientEmail();
    }
}
