package com.ayubo.auth_service.controller;

import com.ayubo.auth_service.model.*;
import com.ayubo.auth_service.repository.UserRepository;
import com.ayubo.auth_service.util.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

// IMPORT THESE FOR THE COOKIES TO WORK!
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already in use!");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        if ("PROVIDER".equalsIgnoreCase(request.getRole())) {
            MedicalProvider provider = new MedicalProvider();
            provider.setFirstName(request.getFirstName());
            provider.setLastName(request.getLastName());
            provider.setEmail(request.getEmail());
            provider.setPhone(request.getPhone());
            provider.setPassword(encodedPassword);
            provider.setRole("PROVIDER");
            provider.setSpecialty(request.getSpecialty());
            provider.setMedicalLicenseNumber(request.getMedicalLicenseNumber());

            userRepository.save(provider);

        } else {
            Patient patient = new Patient();
            patient.setFirstName(request.getFirstName());
            patient.setLastName(request.getLastName());
            patient.setEmail(request.getEmail());
            patient.setPhone(request.getPhone());
            patient.setPassword(encodedPassword);
            patient.setRole("PATIENT");
            patient.setDateOfBirth(request.getDateOfBirth());

            userRepository.save(patient);
        }

        return ResponseEntity.ok("Registration Successful!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {

        var optionalUser = userRepository.findByEmail(request.getEmail());

        if (optionalUser.isEmpty() || !passwordEncoder.matches(request.getPassword(), optionalUser.get().getPassword())) {
            return ResponseEntity.status(401).body("Error: Invalid email or password");
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

        // 4. Return ONLY the role in the JSON body
        return ResponseEntity.ok("{\"role\": \"" + user.getRole() + "\", \"message\": \"Login Successful!\"}");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // 1. Create a "blank" cookie with the exact same name
        Cookie jwtCookie = new Cookie("ayubo_jwt", null);

        // 2. Set the exact same settings so it overwrites the real one
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");

        // 3. Set max age to 0 seconds so the browser deletes it instantly!
        jwtCookie.setMaxAge(0);

        // 4. Attach the self-destructing cookie to the response
        response.addCookie(jwtCookie);

        return ResponseEntity.ok("{\"message\": \"Logged out successfully!\"}");
    }
}