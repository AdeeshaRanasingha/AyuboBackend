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
import com.healthcare.appointmentservice.dto.SlotStatusResponse;
import com.healthcare.appointmentservice.entity.Appointment;
import com.healthcare.appointmentservice.entity.AppointmentStatus;
import com.healthcare.appointmentservice.exception.BadRequestException;
import com.healthcare.appointmentservice.exception.ForbiddenException;
import com.healthcare.appointmentservice.exception.ResourceNotFoundException;
import com.healthcare.appointmentservice.repository.AppointmentRepository;
import com.healthcare.appointmentservice.security.SecurityUtils;
import com.healthcare.appointmentservice.service.AppointmentService;
import com.healthcare.appointmentservice.dto.PaymentStatusUpdateRequest;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final NotificationServiceClient notificationServiceClient;
    private final AppointmentSecurityProperties appointmentSecurityProperties;
    private final DoctorScheduleSlotClient doctorScheduleSlotClient;
    private final AuthProviderDirectoryClient authProviderDirectoryClient;
    private final JdbcTemplate jdbcTemplate;
    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${app.payment-link:${app.frontend-base-url:http://localhost:5173}/patient-dashboard}")
    private String paymentLink;

    @Value("${app.auth-schedule-db-name:ayubo_auth_db}")
    private String authScheduleDbName;

    @Override
    public AppointmentResponse createAppointment(AppointmentCreateRequest request) {
        String patientEmail = resolvePatientEmail(request);
        String contactEmail = normalizeContactEmail(request.getEmail(), patientEmail);

        validateTimeRange(request.getStartTime(), request.getEndTime());

        int maxPatientsForSlot = resolveMaxPatientsForSlot(
                request.getDoctorId(),
                request.getAppointmentDate(),
                request.getStartTime()
        );

        long activeBookings = appointmentRepository.countByDoctorIdAndAppointmentDateAndStartTimeAndStatusIn(
                request.getDoctorId(),
                request.getAppointmentDate(),
                request.getStartTime(),
                List.of(
                        AppointmentStatus.PENDING_PAYMENT,
                        AppointmentStatus.CONFIRMED,
                        AppointmentStatus.COMPLETED,
                        AppointmentStatus.RESCHEDULED
                )
        );

        if (activeBookings >= maxPatientsForSlot) {
            throw new BadRequestException("This slot is sold out for the selected doctor");
        }

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
                .onGoingNumber(Boolean.TRUE.equals(request.getOnGoingNumber()))
                .slotId(resolveSlotId(request.getDoctorId(), request.getAppointmentDate(), request.getStartTime(), request.getSlotId()))
                .status(AppointmentStatus.PENDING_PAYMENT)
                .paymentStatus("PENDING")
                .build();

        Appointment saved = appointmentRepository.save(appointment);

        sendAppointmentCreatedNotification(saved);

        return mapToResponse(saved);
    }

    @Override
    public AppointmentResponse getAppointmentById(Long id) {
        Appointment appointment = findAppointmentById(id);
        assertCanAccessAppointment(appointment);
        Map<Long, Map<String, Object>> doctorMap = fetchProviderMap(appointment.getDoctorId());
        return mapToResponse(appointment, doctorMap);
    }

    @Override
    public List<AppointmentResponse> getMyAppointments() {
        SecurityUtils.requirePatient();
        String email = SecurityUtils.requireCurrentUserEmail();
        List<Appointment> appointments = appointmentRepository.findByPatientEmailIgnoreCase(email);
        Map<Long, Map<String, Object>> doctorMap = fetchProviderMap(null);
        return appointments.stream()
                .map(a -> mapToResponse(a, doctorMap))
                .collect(Collectors.toList());
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
                .collect(Collectors.toList());
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

        if (scheduleChanged) {
            int maxPatientsForSlot = resolveMaxPatientsForSlot(appointment.getDoctorId(), newDate, newStartTime);
            long activeBookings = appointmentRepository.countByDoctorIdAndAppointmentDateAndStartTimeAndStatusInAndIdNot(
                    appointment.getDoctorId(),
                    newDate,
                    newStartTime,
                    List.of(
                            AppointmentStatus.PENDING_PAYMENT,
                            AppointmentStatus.CONFIRMED,
                            AppointmentStatus.COMPLETED,
                            AppointmentStatus.RESCHEDULED
                    ),
                    appointment.getId()
            );
            if (activeBookings >= maxPatientsForSlot) {
                throw new BadRequestException("Requested new slot is sold out");
            }
        }

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


        if (request.getOnGoingNumber() != null) {
            appointment.setOnGoingNumber(request.getOnGoingNumber());
        }

        if (scheduleChanged) {
            appointment.setStatus(AppointmentStatus.RESCHEDULED);
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

        if (newStatus == AppointmentStatus.CONFIRMED && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            int maxPatientsForSlot = resolveMaxPatientsForSlot(
                    appointment.getDoctorId(),
                    appointment.getAppointmentDate(),
                    appointment.getStartTime()
            );
            long alreadyConfirmed = appointmentRepository.countByDoctorIdAndAppointmentDateAndStartTimeAndStatusIn(
                    appointment.getDoctorId(),
                    appointment.getAppointmentDate(),
                    appointment.getStartTime(),
                    List.of(
                            AppointmentStatus.CONFIRMED,
                            AppointmentStatus.COMPLETED,
                            AppointmentStatus.RESCHEDULED
                    )
            );
            if (alreadyConfirmed >= maxPatientsForSlot) {
                throw new BadRequestException("Cannot confirm: slot is already sold out");
            }
        }

        appointment.setStatus(newStatus);


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
    public void cancelAppointment(Long id) {
        Appointment appointment = findAppointmentById(id);
        assertCanModifyAppointmentAsPatientOrProvider(appointment);

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Completed appointment cannot be cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);


        appointmentRepository.save(appointment);

        sendSimpleNotification(
                appointment,
                "Appointment Cancelled",
                "Appointment " + appointment.getAppointmentNumber() + " has been cancelled"
        );
    }

    @Override
    public List<SlotStatusResponse> getAvailableSlots(Long doctorId, String date, boolean forCurrentMonth) {
        LocalDate anchor = parseSlotDate(date);
        if (!forCurrentMonth) {
            List<Appointment> bookedAppointments =
                    appointmentRepository.findByDoctorIdAndAppointmentDate(doctorId, anchor);
            return buildAvailableSlotResponsesForDay(doctorId, anchor, bookedAppointments);
        }

        LocalDate monthStart = anchor.withDayOfMonth(1);
        LocalDate monthEnd = anchor.withDayOfMonth(anchor.lengthOfMonth());
        List<Appointment> monthBookings = appointmentRepository.findByDoctorIdAndAppointmentDateBetween(
                doctorId,
                monthStart,
                monthEnd
        );

        Map<LocalDate, List<Appointment>> bookingsByDay = monthBookings.stream()
                .collect(Collectors.groupingBy(Appointment::getAppointmentDate));

        List<SlotStatusResponse> combined = new ArrayList<>();
        for (LocalDate d = monthStart; !d.isAfter(monthEnd); d = d.plusDays(1)) {
            List<Appointment> bookedForDay = bookingsByDay.getOrDefault(d, Collections.emptyList());
            for (SlotStatusResponse res : buildAvailableSlotResponsesForDay(doctorId, d, bookedForDay)) {
                // Prefix status if needed or just add it
                SlotStatusResponse prefixed = SlotStatusResponse.builder()
                        .startTime(d + " " + res.getStartTime())
                        .maxPatients(res.getMaxPatients())
                        .availableSlots(res.getAvailableSlots())
                        .status(res.getStatus())
                        .build();
                combined.add(prefixed);
            }
        }
        return combined;
    }

    @Override
    public AppointmentResponse uploadPrescription(Long appointmentId, MultipartFile file) {
        return null;
    }

    /**
     * Returns list of slot responses for {@code day}, using auth schedule capacity
     * minus confirmed appointments for that day.
     */
    private List<SlotStatusResponse> buildAvailableSlotResponsesForDay(
            long doctorId,
            LocalDate day,
            List<Appointment> bookedAppointmentsForDay
    ) {
        // PER REQUIREMENT: Deduce available slots ONLY after doctor approval
        // Approved/Confirmed statuses: CONFIRMED, COMPLETED, RESCHEDULED
        Map<String, Long> confirmedCountBySlot = bookedAppointmentsForDay.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED
                        || a.getStatus() == AppointmentStatus.COMPLETED
                        || a.getStatus() == AppointmentStatus.RESCHEDULED)
                .collect(Collectors.groupingBy(
                        a -> a.getStartTime().toString().substring(0, 5),
                        Collectors.counting()
                ));
        
        List<AuthScheduleSlotRow> scheduledRows = doctorScheduleSlotClient.fetchScheduledSlots(doctorId, day.toString());
        List<SlotStatusResponse> responses = new ArrayList<>();
        
        for (AuthScheduleSlotRow row : scheduledRows) {
            if (row.startTime() == null || row.startTime().trim().isEmpty()) {
                continue;
            }
            String slotKey = normalizeSlotTimeKey(row.startTime());
            int maxCap = row.maxPatients() != null && row.maxPatients() > 0 ? row.maxPatients() : 1;
            long confirmedCount = confirmedCountBySlot.getOrDefault(slotKey, 0L);
            long available = maxCap - confirmedCount;
            if (available < 0) available = 0;

            String status = available > 0 ? "AVAILABLE" : "SOLD OUT";

            responses.add(SlotStatusResponse.builder()
                    .startTime(slotKey)
                    .maxPatients(maxCap)
                    .availableSlots(available)
                    .status(status)
                    .build()
            );
        }

        responses.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));
        return responses;
    }

    private static String normalizeSlotTimeKey(String raw) {
        String s = raw.trim();
        return s.length() >= 5 ? s.substring(0, 5) : s;
    }

    private int resolveMaxPatientsForSlot(long doctorId, LocalDate day, LocalTime startTime) {
        String slotKey = normalizeSlotTimeKey(startTime.toString());
        List<AuthScheduleSlotRow> scheduledRows = doctorScheduleSlotClient.fetchScheduledSlots(doctorId, day.toString());
        for (AuthScheduleSlotRow row : scheduledRows) {
            if (row.startTime() == null || row.startTime().trim().isEmpty()) {
                continue;
            }
            String candidate = normalizeSlotTimeKey(row.startTime());
            if (slotKey.equals(candidate)) {
                Integer maxPatients = row.maxPatients();
                return maxPatients != null && maxPatients > 0 ? maxPatients : 1;
            }
        }
        throw new BadRequestException("Selected slot is not available for the chosen date");
    }

    private Long resolveSlotId(Long doctorId, LocalDate day, LocalTime startTime, Long requestedSlotId) {
        if (requestedSlotId != null) {
            return requestedSlotId;
        }
        String tableName = authScheduleDbName + ".doctor_schedule_slots";
        String sql = "SELECT id FROM " + tableName + " " +
                "WHERE provider_id = ? " +
                "AND (slot_date = ? OR date = ?) " +
                "AND (start_time = ? OR start_time = CONCAT(?, ':00')) " +
                "ORDER BY id DESC LIMIT 1";
        String normalizedTime = normalizeSlotTimeKey(startTime.toString());
        List<Long> ids = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getLong("id"),
                doctorId,
                day,
                day.toString(),
                normalizedTime,
                normalizedTime
        );
        return ids.isEmpty() ? null : ids.get(0);
    }

    private LocalDate parseSlotDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
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
        return mapToResponse(appointment, null);
    }

    private AppointmentResponse mapToResponse(Appointment appointment, Map<Long, Map<String, Object>> doctorMap) {
        AppointmentResponse resp = AppointmentResponse.builder()
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
                .onGoingNumber(appointment.getOnGoingNumber())
                .slotId(appointment.getSlotId())
                .status(appointment.getStatus().name())
                .paymentStatus(appointment.getPaymentStatus())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .prescriptionUrl(appointment.getPrescriptionUrl()) // Added this
                .build();

        if (doctorMap != null && doctorMap.containsKey(appointment.getDoctorId())) {
            Map<String, Object> doc = doctorMap.get(appointment.getDoctorId());
            String fName = extractString(doc, "firstName", "");
            String lName = extractString(doc, "lastName", "");
            String fullName = (fName + " " + lName).trim();
            
            resp.setDoctorName(!fullName.isEmpty() ? fullName : "Dr. " + appointment.getDoctorId());
            resp.setDoctorPhoto(extractString(doc, "profileImage", null));
            resp.setHospitalName(extractString(doc, "hospitalName", appointment.getSpecialty()));
        } else {
            // Fallbacks for UI
            resp.setDoctorName("Dr. " + appointment.getDoctorId());
            resp.setHospitalName(appointment.getSpecialty());
        }

        return resp;
    }

    private Map<Long, Map<String, Object>> fetchProviderMap(Long targetDoctorId) {
        try {
            List<Map<String, Object>> directory = authProviderDirectoryClient.fetchProviderDirectory();
            if (directory == null) return Collections.emptyMap();

            return directory.stream()
                    .filter(m -> m.get("id") != null)
                    .filter(m -> targetDoctorId == null || String.valueOf(targetDoctorId).equals(String.valueOf(m.get("id"))))
                    .collect(Collectors.toMap(
                            m -> Long.valueOf(m.get("id").toString()),
                            m -> m,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.warn("Failed to fetch provider directory for enrichment: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String extractString(Map<String, Object> map, String key, String fallback) {
        Object val = map.get(key);
        return (val != null && !val.toString().isBlank()) ? val.toString().trim() : fallback;
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
        if (appointment.getPatientName() != null && !appointment.getPatientName().trim().isEmpty()) {
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
        if (requestedEmail == null || requestedEmail.trim().isEmpty()) {
            return fallbackEmail;
        }
        return requestedEmail.trim();
    }

    private String resolvePatientEmail(AppointmentCreateRequest request) {
        if (SecurityUtils.hasRole("PATIENT")) {
            return SecurityUtils.requireCurrentUserEmail();
        }
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            return request.getEmail().trim();
        }
        return "guest-" + UUID.randomUUID().toString().substring(0, 8) + "@ayubo.local";
    }

    private String notificationRecipient(Appointment appointment) {
        if (appointment.getContactEmail() != null && !appointment.getContactEmail().trim().isEmpty()) {
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
        if (rawPhone == null || rawPhone.trim().isEmpty()) {
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

    @Override
    public AppointmentResponse getAppointmentPublic(Long id) {
        Appointment appointment = findAppointmentById(id);
        Map<Long, Map<String, Object>> doctorMap = fetchProviderMap(appointment.getDoctorId());
        return mapToResponse(appointment, doctorMap);
    }

    @Override
    public AppointmentResponse updatePaymentStatus(Long id, PaymentStatusUpdateRequest request) {
        Appointment appointment = findAppointmentById(id);

        if (request.getPaymentStatus() != null && !request.getPaymentStatus().isBlank()) {
            appointment.setPaymentStatus(request.getPaymentStatus().trim().toUpperCase());
        }

        if (request.getTotalPrice() != null) {
            appointment.setTotalPrice(request.getTotalPrice().setScale(2, java.math.RoundingMode.HALF_UP));
        }

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            appointment.setNotes(request.getNotes().trim());
        }

        Appointment updated = appointmentRepository.save(appointment);
        return mapToResponse(updated);
    }
}
