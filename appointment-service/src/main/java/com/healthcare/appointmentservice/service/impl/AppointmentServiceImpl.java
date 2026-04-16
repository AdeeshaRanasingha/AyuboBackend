package com.healthcare.appointmentservice.service.impl;

import com.healthcare.appointmentservice.client.AuthProviderDirectoryClient;
import com.healthcare.appointmentservice.client.DoctorScheduleSlotClient;
import com.healthcare.appointmentservice.client.NotificationServiceClient;
import com.healthcare.appointmentservice.client.dto.AuthScheduleSlotRow;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final NotificationServiceClient notificationServiceClient;
    private final AppointmentSecurityProperties appointmentSecurityProperties;
    private final DoctorScheduleSlotClient doctorScheduleSlotClient;
    private final AuthProviderDirectoryClient authProviderDirectoryClient;
    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${app.payment-link:${app.frontend-base-url:http://localhost:5173}/patient-dashboard}")
    private String paymentLink;

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
                .contactNumber(normalizeSriLankanPhone(request.getMobile()))
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
        Long mappedDoctorId = requireDoctorIdForProvider(SecurityUtils.requireCurrentUserEmail());
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

        boolean scheduleChanged = !newDate.equals(appointment.getAppointmentDate())
                || !newStartTime.equals(appointment.getStartTime())
                || !newEndTime.equals(appointment.getEndTime());

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

        if (request.getAppointmentFor() != null) {
            appointment.setAppointmentFor(request.getAppointmentFor());
        }

        if (request.getAppointmentType() != null) {
            appointment.setAppointmentType(request.getAppointmentType());
        }

        if (request.getTitle() != null) {
            appointment.setPatientTitle(request.getTitle());
        }

        if (request.getName() != null) {
            appointment.setPatientName(request.getName());
        }

        if (request.getMobile() != null) {
            appointment.setContactNumber(normalizeSriLankanPhone(request.getMobile()));
        }

        if (request.getIdType() != null) {
            appointment.setIdentificationType(request.getIdType());
        }

        if (request.getIdValue() != null) {
            appointment.setIdentificationValue(request.getIdValue());
        }

        if (request.getEmail() != null) {
            appointment.setContactEmail(normalizeContactEmail(request.getEmail(), appointment.getPatientEmail()));
        }

        if (request.getReason() != null) {
            appointment.setReason(request.getReason());
        }

        if (request.getNoteOrAddress() != null) {
            appointment.setNoteOrAddress(request.getNoteOrAddress());
        }

        if (request.getNoShowRefund() != null) {
            appointment.setNoShowRefund(request.getNoShowRefund());
        }

        if (request.getOnGoingNumber() != null) {
            appointment.setOnGoingNumber(request.getOnGoingNumber());
        }

        if (scheduleChanged) {
            appointment.setStatus(AppointmentStatus.RESCHEDULED);
            appointment.setRescheduleCount(appointment.getRescheduleCount() + 1);
            appointment.setNotes("Appointment rescheduled");
        } else {
            appointment.setNotes("Appointment details updated");
        }

        Appointment updated = appointmentRepository.save(appointment);

        if (scheduleChanged) {
            sendSimpleNotification(
                    updated,
                    "Appointment Rescheduled",
                    "Your appointment " + updated.getAppointmentNumber() + " has been rescheduled to " +
                            updated.getAppointmentDate() + " " + updated.getStartTime()
            );
        } else {
            sendSimpleNotification(
                    updated,
                    "Appointment Updated",
                    "Your appointment " + updated.getAppointmentNumber() + " details have been updated"
            );
        }

        return mapToResponse(updated);
    }

    @Override
    public AppointmentResponse updateStatus(Long id, StatusUpdateRequest request) {
        SecurityUtils.requireProvider();
        Appointment appointment = findAppointmentById(id);
        Long mappedDoctorId = requireDoctorIdForProvider(SecurityUtils.requireCurrentUserEmail());
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

        Appointment updated = appointmentRepository.save(appointment);

        if (newStatus == AppointmentStatus.CONFIRMED) {
            // Doctor approval flow: notify patient to proceed with payment from dashboard.
            appointment.setPaymentStatus("PENDING");
            updated = appointmentRepository.save(appointment);

            sendAppointmentConfirmedNotification(updated);
        } else if (newStatus == AppointmentStatus.CANCELLED || newStatus == AppointmentStatus.REJECTED) {
            sendAppointmentCancelledNotification(updated);
        } else {
            sendSimpleNotification(
                    updated,
                    "Appointment Status Updated",
                    "Appointment " + updated.getAppointmentNumber() + " status changed to " + updated.getStatus()
            );
        }

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
                appointment,
                "Appointment Cancelled",
                "Appointment " + appointment.getAppointmentNumber() + " has been cancelled"
        );
    }

    @Override
    public List<String> getAvailableSlots(Long doctorId, String date, boolean forCurrentMonth) {
        LocalDate anchor = parseSlotDate(date);
        if (!forCurrentMonth) {
            List<Appointment> bookedAppointments =
                    appointmentRepository.findByDoctorIdAndAppointmentDate(doctorId, anchor);
            return buildAvailableSlotTimesForDay(doctorId, anchor, bookedAppointments);
        }

        LocalDate monthStart = anchor.withDayOfMonth(1);
        LocalDate monthEnd = anchor.withDayOfMonth(anchor.lengthOfMonth());
        List<Appointment> monthBookings = appointmentRepository.findByDoctorIdAndAppointmentDateBetween(
                doctorId,
                monthStart,
                monthEnd
        );

        Map<LocalDate, Map<String, Long>> bookedByDay = monthBookings.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED
                        && a.getStatus() != AppointmentStatus.REJECTED)
                .collect(Collectors.groupingBy(
                        Appointment::getAppointmentDate,
                        Collectors.groupingBy(
                                a -> a.getStartTime().toString().substring(0, 5),
                                Collectors.counting()
                        )
                ));

        List<String> combined = new ArrayList<>();
        for (LocalDate d = monthStart; !d.isAfter(monthEnd); d = d.plusDays(1)) {
            Map<String, Long> bookedForDay = bookedByDay.getOrDefault(d, Map.of());
            for (String time : buildAvailableSlotTimesForDay(doctorId, d, bookedForDay)) {
                combined.add(d + " " + time);
            }
        }
        combined.sort(String::compareTo);
        return combined;
    }

    /**
     * Returns start times (HH:mm) still open for booking on {@code day}, using auth schedule capacity
     * minus non-cancelled appointments for that day.
     */
    private List<String> buildAvailableSlotTimesForDay(
            long doctorId,
            LocalDate day,
            List<Appointment> bookedAppointmentsForDay
    ) {
        Map<String, Long> bookedCountBySlot = bookedAppointmentsForDay.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED
                        && a.getStatus() != AppointmentStatus.REJECTED)
                .collect(Collectors.groupingBy(
                        a -> a.getStartTime().toString().substring(0, 5),
                        Collectors.counting()
                ));
        return buildAvailableSlotTimesForDay(doctorId, day, bookedCountBySlot);
    }

    private List<String> buildAvailableSlotTimesForDay(
            long doctorId,
            LocalDate day,
            Map<String, Long> bookedCountBySlot
    ) {
        List<AuthScheduleSlotRow> scheduledRows = doctorScheduleSlotClient.fetchScheduledSlots(doctorId, day.toString());
        Map<String, Integer> capacityByStart = new LinkedHashMap<>();
        for (AuthScheduleSlotRow row : scheduledRows) {
            if (row.startTime() == null || row.startTime().isBlank()) {
                continue;
            }
            String slotKey = normalizeSlotTimeKey(row.startTime());
            int cap = row.maxPatients() != null && row.maxPatients() > 0 ? row.maxPatients() : 1;
            capacityByStart.merge(slotKey, cap, Integer::max);
        }

        if (capacityByStart.isEmpty()) {
            return List.of();
        }

        return capacityByStart.entrySet().stream()
                .filter(e -> bookedCountBySlot.getOrDefault(e.getKey(), 0L) < e.getValue())
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    private static String normalizeSlotTimeKey(String raw) {
        String s = raw.trim();
        return s.length() >= 5 ? s.substring(0, 5) : s;
    }

    private LocalDate parseSlotDate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("date is required");
        }
        String trimmed = raw.trim();
        String candidate = trimmed;
        if (trimmed.length() >= 10 && (trimmed.charAt(4) == '-' || trimmed.charAt(4) == '/')) {
            candidate = trimmed.substring(0, 10);
        }
        try {
            return LocalDate.parse(candidate, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDate.parse(candidate, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            } catch (DateTimeParseException ignored) {
                throw new BadRequestException("Invalid date format. Expected yyyy-MM-dd.");
            }
        }
    }

    private void assertCanAccessAppointment(Appointment appointment) {
        String email = SecurityUtils.requireCurrentUserEmail();
        if (SecurityUtils.hasRole("PATIENT") && email.equalsIgnoreCase(appointment.getPatientEmail())) {
            return;
        }
        if (SecurityUtils.hasRole("PROVIDER")) {
            Optional<Long> mapped = resolveDoctorIdForProviderEmail(email);
            if (mapped.isPresent() && mapped.get().equals(appointment.getDoctorId())) {
                return;
            }
        }
        throw new ForbiddenException("Not allowed to access this appointment");
    }

    private void assertCanModifyAppointmentAsPatientOrProvider(Appointment appointment) {
        String email = SecurityUtils.requireCurrentUserEmail();
        boolean asPatient = SecurityUtils.hasRole("PATIENT") && email.equalsIgnoreCase(appointment.getPatientEmail());
        boolean asProvider = SecurityUtils.hasRole("PROVIDER")
                && resolveDoctorIdForProviderEmail(email)
                .map(d -> d.equals(appointment.getDoctorId()))
                .orElse(false);
        if (!asPatient && !asProvider) {
            throw new ForbiddenException("Not allowed to modify this appointment");
        }
    }

    private Optional<Long> resolveDoctorIdForProviderEmail(String email) {
        Optional<Long> yaml = appointmentSecurityProperties.doctorIdForProviderEmail(email);
        if (yaml.isPresent()) {
            return yaml;
        }
        return authProviderDirectoryClient.findProviderIdByEmail(email);
    }

    private Long requireDoctorIdForProvider(String email) {
        return resolveDoctorIdForProviderEmail(email)
                .orElseThrow(() -> new ForbiddenException("No doctor profile mapped for this provider account"));
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
                .totalPrice(appointment.getTotalPrice())
                .notes(appointment.getNotes())
                .cancelReason(appointment.getCancelReason())
                .rescheduleCount(appointment.getRescheduleCount())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }

    private void sendAppointmentCreatedNotification(Appointment appointment) {
        sendSimpleNotification(
                appointment,
                "Appointment Created",
                buildScheduledAppointmentMessage(appointment)
        );
    }

    private void sendAppointmentConfirmedNotification(Appointment appointment) {
        sendSimpleNotification(
                appointment,
                "Appointment Confirmed",
                buildScheduledAppointmentMessage(appointment) + "\n\n" +
                        "Please note that payment must be settled prior to the session. " +
                        "You can securely pay via our portal: " + paymentLink + ".\n\n" +
                        "Thank you for choosing Ayubo."
        );
    }

    private void sendAppointmentCancelledNotification(Appointment appointment) {
        String doctorLabel = doctorLabel(appointment);
        String patientName = patientName(appointment);
        String message = "Dear " + patientName + ", your appointment with " + doctorLabel +
                " on " + formatAppointmentDateTime(appointment) + " has been cancelled.";
        sendSimpleNotification(appointment, "Appointment Cancelled", message);
    }

    private void sendSimpleNotification(Appointment appointment, String subject, String message) {
        NotificationRequest request = NotificationRequest.builder()
                .recipientEmail(notificationRecipient(appointment))
                .recipientPhone(normalizeSriLankanPhone(appointment.getContactNumber()))
                .subject(subject)
                .message(message)
                .build();

        notificationServiceClient.sendNotification(request);
    }

    private String buildScheduledAppointmentMessage(Appointment appointment) {
        String patientName = patientName(appointment);
        String doctorLabel = doctorLabel(appointment);
        return "Dear " + patientName + ", your appointment with " + doctorLabel +
                " has been successfully scheduled for " + formatAppointmentDateTime(appointment) + ".";
    }

    private String patientName(Appointment appointment) {
        if (appointment.getPatientName() != null && !appointment.getPatientName().isBlank()) {
            return appointment.getPatientName().trim();
        }
        return "Patient";
    }

    private String doctorLabel(Appointment appointment) {
        return "Dr. " + appointment.getDoctorId();
    }

    private String formatAppointmentDateTime(Appointment appointment) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        return appointment.getAppointmentDate().format(dateFormatter) + " at " + appointment.getStartTime().format(timeFormatter);
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

    /**
     * Normalizes common Sri Lankan formats to E.164 required by SMS providers:
     * - 07XXXXXXXX -> +947XXXXXXXX
     * - 94XXXXXXXXX -> +94XXXXXXXXX
     * - +94XXXXXXXXX -> +94XXXXXXXXX
     */
    private String normalizeSriLankanPhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }

        String cleaned = rawPhone.replaceAll("[^\\d+]", "");
        if (cleaned.startsWith("+94")) {
            return cleaned;
        }

        if (cleaned.startsWith("94")) {
            return "+" + cleaned;
        }

        if (cleaned.startsWith("0") && cleaned.length() == 10) {
            return "+94" + cleaned.substring(1);
        }

        // Fallback: return original cleaned string so existing non-SL formats still pass through.
        return cleaned;
    }
}
