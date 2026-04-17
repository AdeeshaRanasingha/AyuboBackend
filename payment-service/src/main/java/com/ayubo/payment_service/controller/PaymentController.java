package com.ayubo.payment_service.controller;

import com.ayubo.payment_service.dto.PaymentRequest;
import com.ayubo.payment_service.entity.TransactionRecord;
import com.ayubo.payment_service.repository.TransactionRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${services.appointment.base-url}")
    private String appointmentBaseUrl;

    @Value("${services.auth.base-url}")
    private String authBaseUrl;

    @Value("${services.notification.base-url}")
    private String notificationBaseUrl;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public PaymentController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(@RequestBody PaymentRequest request) {
        try {
            if (request.getAppointmentId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "appointmentId is required"));
            }

            Map<String, Object> appointmentWrapper = restTemplate.getForObject(
                    appointmentBaseUrl + "/api/appointments/public/" + request.getAppointmentId(),
                    Map.class
            );
            Map<String, Object> appointment = unwrapData(appointmentWrapper);

            if (appointment == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Appointment not found"));
            }

            String appointmentStatus = stringValue(appointment.get("status"));
            String paymentStatus = stringValue(appointment.get("paymentStatus"));

            if (!"CONFIRMED".equalsIgnoreCase(appointmentStatus)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Payment is allowed only after doctor confirmation"));
            }

            if ("PAID".equalsIgnoreCase(paymentStatus)) {
                return ResponseEntity.badRequest().body(Map.of("error", "This appointment is already paid"));
            }

            Long doctorId = longValue(appointment.get("doctorId"));
            String appointmentType = stringValue(appointment.get("appointmentType"));
            String appointmentNumber = stringValue(appointment.get("appointmentNumber"));
            String patientEmail = stringValue(appointment.get("patientEmail"));
            String patientName = stringValue(appointment.get("patientName"));
            String patientPhone = stringValue(appointment.get("contactNumber"));

            Map<String, Object> provider = restTemplate.getForObject(
                    authBaseUrl + "/api/provider/" + doctorId + "/billing-summary",
                    Map.class
            );

            Map<String, Object> fees = restTemplate.getForObject(
                    authBaseUrl + "/api/public/fees",
                    Map.class
            );

            if (provider == null || fees == null) {
                return ResponseEntity.status(500).body(Map.of("error", "Unable to load billing configuration"));
            }

            String doctorName = stringValue(provider.get("fullName"));
            String doctorEmail = stringValue(provider.get("email"));
            String doctorPhone = stringValue(provider.get("phone"));
            String hospitalName = stringValue(provider.get("hospitalName"));
            double doctorFee = doubleValue(provider.get("consultationFee"));
            double onlineFee = doubleValue(fees.get("onlineConsultationFee"));
            double taxPercentage = doubleValue(fees.get("taxPercentage"));

            double facilityFee;
            if ("ONLINE".equalsIgnoreCase(appointmentType) || "TELEMEDICINE".equalsIgnoreCase(appointmentType)) {
                facilityFee = onlineFee;
            } else {
                Map<String, Object> hospitalFees = safeMap(fees.get("hospitalFees"));
                facilityFee = doubleValue(hospitalFees.getOrDefault(hospitalName, 0));
            }

            BigDecimal subtotal = BigDecimal.valueOf(doctorFee + facilityFee);
            BigDecimal taxAmount = subtotal
                    .multiply(BigDecimal.valueOf(taxPercentage))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal totalAmount = subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

            Stripe.apiKey = stripeApiKey;

            String invoiceNumber = "INV-" + request.getAppointmentId() + "-" + System.currentTimeMillis();

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendBaseUrl + "/payment-success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendBaseUrl + "/patient-dashboard")
                    .putMetadata("appointmentId", String.valueOf(request.getAppointmentId()))
                    .putMetadata("appointmentNumber", appointmentNumber)
                    .putMetadata("invoiceNumber", invoiceNumber)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("lkr")
                                                    .setUnitAmount(totalAmount.multiply(BigDecimal.valueOf(100)).longValue())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Consultation Payment - " + appointmentNumber)
                                                                    .setDescription("Doctor: " + doctorName)
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);

            TransactionRecord newTx = new TransactionRecord();
            newTx.setAppointmentId(request.getAppointmentId());
            newTx.setDoctorId(doctorId);
            newTx.setAppointmentNumber(appointmentNumber);
            newTx.setAppointmentType(appointmentType);
            newTx.setHospitalName(hospitalName);
            newTx.setPatientEmail(patientEmail);
            newTx.setPatientName(patientName);
            newTx.setPatientPhone(patientPhone);
            newTx.setDoctorName(doctorName);
            newTx.setDoctorEmail(doctorEmail);
            newTx.setDoctorPhone(doctorPhone);
            newTx.setDoctorFee(doctorFee);
            newTx.setFacilityFee(facilityFee);
            newTx.setTaxPercentage(taxPercentage);
            newTx.setTaxAmount(taxAmount.doubleValue());
            newTx.setAmount(totalAmount.doubleValue());
            newTx.setCurrency("LKR");
            newTx.setStatus("PENDING");
            newTx.setStripeSessionId(session.getId());
            newTx.setInvoiceNumber(invoiceNumber);

            transactionRepository.save(newTx);

            return ResponseEntity.ok(Map.of(
                    "url", session.getUrl(),
                    "amount", totalAmount,
                    "invoiceNumber", invoiceNumber
            ));

        } catch (StripeException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Stripe failed to create session", "details", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Payment initialization failed", "details", e.getMessage()));
        }
    }

    @GetMapping("/preview/{appointmentId}")
    public ResponseEntity<?> previewPayment(@PathVariable Long appointmentId) {
        try {
            Map<String, Object> appointmentWrapper = restTemplate.getForObject(
                    appointmentBaseUrl + "/api/appointments/public/" + appointmentId,
                    Map.class
            );
            Map<String, Object> appointment = unwrapData(appointmentWrapper);

            if (appointment == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Appointment not found"));
            }

            String appointmentStatus = stringValue(appointment.get("status"));
            String paymentStatus = stringValue(appointment.get("paymentStatus"));

            if (!"CONFIRMED".equalsIgnoreCase(appointmentStatus)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invoice can be generated only after doctor confirmation"));
            }

            if ("PAID".equalsIgnoreCase(paymentStatus)) {
                TransactionRecord existingTx = transactionRepository.findTopByAppointmentIdOrderByCreatedAtDesc(appointmentId);
                if (existingTx != null) {
                    Map<String, Object> alreadyPaid = new LinkedHashMap<>();
                    alreadyPaid.put("appointmentId", existingTx.getAppointmentId());
                    alreadyPaid.put("appointmentNumber", existingTx.getAppointmentNumber());
                    alreadyPaid.put("invoiceNumber", existingTx.getInvoiceNumber());
                    alreadyPaid.put("status", existingTx.getStatus());
                    alreadyPaid.put("patientName", existingTx.getPatientName());
                    alreadyPaid.put("patientEmail", existingTx.getPatientEmail());
                    alreadyPaid.put("doctorName", existingTx.getDoctorName());
                    alreadyPaid.put("doctorFee", existingTx.getDoctorFee());
                    alreadyPaid.put("facilityFee", existingTx.getFacilityFee());
                    alreadyPaid.put("taxPercentage", existingTx.getTaxPercentage());
                    alreadyPaid.put("taxAmount", existingTx.getTaxAmount());
                    alreadyPaid.put("amount", existingTx.getAmount());
                    alreadyPaid.put("currency", existingTx.getCurrency());
                    alreadyPaid.put("appointmentType", existingTx.getAppointmentType());
                    alreadyPaid.put("hospitalName", existingTx.getHospitalName());
                    alreadyPaid.put("paidAt", existingTx.getPaidAt());
                    alreadyPaid.put("alreadyPaid", true);
                    return ResponseEntity.ok(alreadyPaid);
                }
            }

            Long doctorId = longValue(appointment.get("doctorId"));
            String appointmentType = stringValue(appointment.get("appointmentType"));
            String appointmentNumber = stringValue(appointment.get("appointmentNumber"));
            String patientEmail = stringValue(appointment.get("patientEmail"));
            String patientName = stringValue(appointment.get("patientName"));
            String patientPhone = stringValue(appointment.get("contactNumber"));

            Map<String, Object> provider = restTemplate.getForObject(
                    authBaseUrl + "/api/provider/" + doctorId + "/billing-summary",
                    Map.class
            );

            Map<String, Object> fees = restTemplate.getForObject(
                    authBaseUrl + "/api/public/fees",
                    Map.class
            );

            if (provider == null || fees == null) {
                return ResponseEntity.status(500).body(Map.of("error", "Unable to load billing configuration"));
            }

            String doctorName = stringValue(provider.get("fullName"));
            String doctorEmail = stringValue(provider.get("email"));
            String doctorPhone = stringValue(provider.get("phone"));
            String hospitalName = stringValue(provider.get("hospitalName"));
            double doctorFee = doubleValue(provider.get("consultationFee"));
            double onlineFee = doubleValue(fees.get("onlineConsultationFee"));
            double taxPercentage = doubleValue(fees.get("taxPercentage"));

            double facilityFee;
            if ("ONLINE".equalsIgnoreCase(appointmentType) || "TELEMEDICINE".equalsIgnoreCase(appointmentType)) {
                facilityFee = onlineFee;
            } else {
                Map<String, Object> hospitalFees = safeMap(fees.get("hospitalFees"));
                facilityFee = doubleValue(hospitalFees.getOrDefault(hospitalName, 0));
            }

            BigDecimal subtotal = BigDecimal.valueOf(doctorFee + facilityFee).setScale(2, RoundingMode.HALF_UP);
            BigDecimal taxAmount = subtotal
                    .multiply(BigDecimal.valueOf(taxPercentage))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal totalAmount = subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

            String previewInvoiceNumber = "INV-PREVIEW-" + appointmentId;

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("appointmentId", appointmentId);
            response.put("appointmentNumber", appointmentNumber);
            response.put("invoiceNumber", previewInvoiceNumber);
            response.put("patientName", patientName);
            response.put("patientEmail", patientEmail);
            response.put("patientPhone", patientPhone);
            response.put("doctorId", doctorId);
            response.put("doctorName", doctorName);
            response.put("doctorEmail", doctorEmail);
            response.put("doctorPhone", doctorPhone);
            response.put("appointmentType", appointmentType);
            response.put("hospitalName", hospitalName);
            response.put("doctorFee", doctorFee);
            response.put("facilityFee", facilityFee);
            response.put("taxPercentage", taxPercentage);
            response.put("taxAmount", taxAmount);
            response.put("amount", totalAmount);
            response.put("currency", "LKR");
            response.put("alreadyPaid", false);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Unable to generate payment preview",
                    "details", e.getMessage()
            ));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestParam String sessionId) {
        try {
            Stripe.apiKey = stripeApiKey;

            Session stripeSession = Session.retrieve(sessionId);
            TransactionRecord tx = transactionRepository.findByStripeSessionId(sessionId);

            if (tx == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Transaction not found"));
            }

            String paymentStatus = stripeSession.getPaymentStatus();
            if (!"paid".equalsIgnoreCase(paymentStatus)) {
                tx.setStatus("FAILED");
                transactionRepository.save(tx);
                return ResponseEntity.badRequest().body(Map.of("error", "Payment is not completed"));
            }

            String realEmail = tx.getPatientEmail();
            if (stripeSession.getCustomerDetails() != null && stripeSession.getCustomerDetails().getEmail() != null) {
                realEmail = stripeSession.getCustomerDetails().getEmail();
            }

            tx.setStatus("PAID");
            tx.setPaidAt(LocalDateTime.now());
            tx.setPatientEmail(realEmail);
            transactionRepository.save(tx);

            Map<String, Object> paymentStatusBody = new LinkedHashMap<>();
            paymentStatusBody.put("paymentStatus", "PAID");
            paymentStatusBody.put("totalPrice", BigDecimal.valueOf(tx.getAmount()).setScale(2, RoundingMode.HALF_UP));
            paymentStatusBody.put("notes", "Payment completed successfully. Invoice: " + tx.getInvoiceNumber());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.exchange(
                    appointmentBaseUrl + "/api/appointments/" + tx.getAppointmentId() + "/payment-status",
                    HttpMethod.PATCH,
                    new HttpEntity<>(paymentStatusBody, headers),
                    Map.class
            );

            sendNotification(
                    tx.getPatientEmail(),
                    tx.getPatientPhone(),
                    "Appointment Payment Confirmed",
                    buildPatientInvoiceMessage(tx)
            );

            sendNotification(
                    tx.getDoctorEmail(),
                    tx.getDoctorPhone(),
                    "Patient Payment Received",
                    buildDoctorMessage(tx)
            );

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", "Payment verified successfully");
            response.put("status", tx.getStatus());
            response.put("appointmentId", tx.getAppointmentId());
            response.put("appointmentNumber", tx.getAppointmentNumber());
            response.put("invoiceNumber", tx.getInvoiceNumber());
            response.put("amount", tx.getAmount());
            response.put("currency", tx.getCurrency());
            response.put("doctorName", tx.getDoctorName());
            response.put("appointmentType", tx.getAppointmentType());
            response.put("hospitalName", tx.getHospitalName());
            response.put("patientEmail", tx.getPatientEmail());
            response.put("paidAt", tx.getPaidAt());
            response.put("doctorFee", tx.getDoctorFee());
            response.put("facilityFee", tx.getFacilityFee());
            response.put("taxAmount", tx.getTaxAmount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Error verifying payment",
                    "details", e.getMessage()
            ));
        }
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<?> getLatestTransactionByAppointment(@PathVariable Long appointmentId) {
        TransactionRecord tx = transactionRepository.findTopByAppointmentIdOrderByCreatedAtDesc(appointmentId);
        if (tx == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("appointmentId", tx.getAppointmentId());
        response.put("appointmentNumber", tx.getAppointmentNumber());
        response.put("invoiceNumber", tx.getInvoiceNumber());
        response.put("status", tx.getStatus());
        response.put("amount", tx.getAmount());
        response.put("currency", tx.getCurrency());
        response.put("doctorName", tx.getDoctorName());
        response.put("patientEmail", tx.getPatientEmail());
        response.put("doctorFee", tx.getDoctorFee());
        response.put("facilityFee", tx.getFacilityFee());
        response.put("taxAmount", tx.getTaxAmount());
        response.put("paidAt", tx.getPaidAt());

        return ResponseEntity.ok(response);
    }

    private void sendNotification(String email, String phone, String subject, String message) {
        try {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("recipientEmail", email);
            request.put("recipientPhone", phone);
            request.put("subject", subject);
            request.put("message", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.postForEntity(
                    notificationBaseUrl + "/api/notifications/send",
                    new HttpEntity<>(request, headers),
                    Map.class
            );
        } catch (Exception ignored) {
        }
    }

    private String buildPatientInvoiceMessage(TransactionRecord tx) {
        return "Your payment was successful.\n\n" +
                "Invoice Number: " + tx.getInvoiceNumber() + "\n" +
                "Appointment Number: " + tx.getAppointmentNumber() + "\n" +
                "Doctor: " + tx.getDoctorName() + "\n" +
                "Appointment Type: " + tx.getAppointmentType() + "\n" +
                "Hospital: " + tx.getHospitalName() + "\n" +
                "Doctor Fee: LKR " + formatMoney(tx.getDoctorFee()) + "\n" +
                "Facility Fee: LKR " + formatMoney(tx.getFacilityFee()) + "\n" +
                "Tax: LKR " + formatMoney(tx.getTaxAmount()) + "\n" +
                "Total Paid: LKR " + formatMoney(tx.getAmount()) + "\n\n" +
                "Thank you for using Ayubo.";
    }

    private String buildDoctorMessage(TransactionRecord tx) {
        return "Payment received for appointment " + tx.getAppointmentNumber() + ".\n\n" +
                "Patient: " + tx.getPatientName() + "\n" +
                "Invoice Number: " + tx.getInvoiceNumber() + "\n" +
                "Total Paid: LKR " + formatMoney(tx.getAmount());
    }

    private String formatMoney(Double value) {
        return BigDecimal.valueOf(value == null ? 0 : value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrapData(Map<String, Object> wrapper) {
        if (wrapper == null) return null;
        Object data = wrapper.get("data");
        if (data instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeMap(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return new LinkedHashMap<>();
    }

    private String stringValue(Object obj) {
        return obj == null ? "" : String.valueOf(obj);
    }

    private Long longValue(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number number) return number.longValue();
        return Long.parseLong(String.valueOf(obj));
    }

    private double doubleValue(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number number) return number.doubleValue();
        return Double.parseDouble(String.valueOf(obj));
    }
}
