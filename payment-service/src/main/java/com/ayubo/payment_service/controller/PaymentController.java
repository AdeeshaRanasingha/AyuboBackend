package com.ayubo.payment_service.controller;

import com.ayubo.payment_service.entity.TransactionRecord;
import com.ayubo.payment_service.repository.TransactionRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ayubo.payment_service.dto.PaymentRequest;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Autowired
    private TransactionRepository transactionRepository;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(@RequestBody PaymentRequest request) {
        // Sets the Stripe key for the checkout
        Stripe.apiKey = stripeApiKey;

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://localhost:5173/payment-success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl("http://localhost:5173/patient-dashboard")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("lkr")
                                                    .setUnitAmount(250000L) // LKR 2,500.00
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Doctor Appointment - Video Consult")
                                                                    .build())
                                                    .build())
                                    .build())
                    .build();

            Session session = Session.create(params);

            // 2. SAVE TO DATABASE BEFORE REDIRECTING
            TransactionRecord newTx = new TransactionRecord();
            newTx.setAmount(2500.00);
            newTx.setPatientEmail(request.getPatientEmail());

            newTx.setCurrency("LKR");
            newTx.setStatus("PENDING");
            newTx.setStripeSessionId(session.getId());

            transactionRepository.save(newTx); // Pushes to MySQL

            Map<String, String> responseData = new HashMap<>();
            responseData.put("url", session.getUrl());
            return ResponseEntity.ok(responseData);

        } catch (StripeException e) {
            System.out.println("STRIPE ERROR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"error\": \"Stripe failed to create session\"}");
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestParam String sessionId) {
        // <--- FIX 2: We MUST tell this method what the API key is too!
        Stripe.apiKey = stripeApiKey;

        try {
            // 1. REACH OUT TO STRIPE AND GET THE RECEIPT
            Session stripeSession = Session.retrieve(sessionId);

            // Grab the actual email the user typed on the Stripe website!
            String realEmail = stripeSession.getCustomerDetails().getEmail();

            // 2. FIND THE PENDING TRANSACTION IN YOUR DATABASE
            TransactionRecord tx = transactionRepository.findByStripeSessionId(sessionId);

            if (tx == null) {
                return ResponseEntity.badRequest().body("Transaction not found!");
            }

            // 3. OVERWRITE THE PLACEHOLDER AND FLIP TO PAID
            tx.setStatus("PAID");
            tx.setPatientEmail(realEmail); // Overwrites "waiting_for_stripe..."

            // 4. Save the updated row back to MySQL
            transactionRepository.save(tx);

            return ResponseEntity.ok(Map.of("message", "Payment verified and updated to PAID!"));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error verifying payment: " + e.getMessage());
        }
    }
}