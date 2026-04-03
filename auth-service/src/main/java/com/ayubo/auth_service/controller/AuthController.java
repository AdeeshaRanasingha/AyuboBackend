package com.ayubo.auth_service.controller;

import com.ayubo.auth_service.model.*;
import com.ayubo.auth_service.repository.MedicalProviderRepository;
import com.ayubo.auth_service.repository.PatientRepository;
import com.ayubo.auth_service.repository.UserRepository;
import com.ayubo.auth_service.util.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

// IMPORT THESE FOR THE COOKIES TO WORK!
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // --- 1. ADDED MISSING REPOSITORIES ---
    @Autowired
    private MedicalProviderRepository providerRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth Service is LIVE!");
    }

    // --- 2. CHANGED TO <?> TO ALLOW JSON MAPS ---
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already in use!"));
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        if (request.getRole().equalsIgnoreCase("PROVIDER")) {
            MedicalProvider provider = new MedicalProvider();
            provider.setFirstName(request.getFirstName());
            provider.setLastName(request.getLastName());
            provider.setEmail(request.getEmail());
            provider.setPassword(encodedPassword);

            provider.setRole("PROVIDER");
            provider.setSpecialty(request.getSpecialty());
            provider.setMedicalLicenseNumber(request.getMedicalLicenseNumber());
            provider.setHospitalName(request.getHospitalName());


            // Replaced Role.PROVIDER with a String to avoid Enum errors
            provider.setRole("PROVIDER");

            // Map the new fields
            provider.setPhone(request.getPhone());
            provider.setMedicalLicenseNumber(request.getMedicalLicenseNumber());
            provider.setSpecialty(request.getSpecialty());
            provider.setHospitalName(request.getHospitalName());
            provider.setYearsOfExperience(request.getYearsOfExperience());
            provider.setQualifications(request.getQualifications());
            provider.setConsultationFee(request.getConsultationFee());
            provider.setBio(request.getBio());

            provider.setIsApproved(false); // Force them to be approved by Admin!

            providerRepository.save(provider);
            return ResponseEntity.ok(Map.of("message", "Doctor registered successfully! Pending admin approval."));

        } else {
            // --- 3. ADDED LOGIC TO ACTUALLY SAVE PATIENTS! ---
            Patient patient = new Patient();
            patient.setFirstName(request.getFirstName());
            patient.setLastName(request.getLastName());
            patient.setEmail(request.getEmail());
            patient.setPassword(encodedPassword);
            patient.setRole("PATIENT");

            patientRepository.save(patient);
            return ResponseEntity.ok(Map.of("message", "Patient registered successfully!"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {

        var optionalUser = userRepository.findByEmail(request.getEmail());

        if (optionalUser.isEmpty() || !passwordEncoder.matches(request.getPassword(), optionalUser.get().getPassword())) {
            // Updated to return valid JSON
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }

        User user = optionalUser.get();

        // 1. Generate the VIP Wristband
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        // 2. Pack it into an HttpOnly Cookie
        Cookie jwtCookie = new Cookie("ayubo_jwt", token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(10 * 60 * 60);

        // 3. Attach the cookie to the HTTP response
        response.addCookie(jwtCookie);


        // 4. Return the token, role, and message in the JSON body (for API clients and Postman)
        return ResponseEntity.ok("{\"token\": \"" + token + "\", \"role\": \"" + user.getRole() + "\", \"message\": \"Login Successful!\"}");

    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie jwtCookie = new Cookie("ayubo_jwt", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);

        return ResponseEntity.ok(Map.of("message", "Logged out successfully!"));
    }
}
