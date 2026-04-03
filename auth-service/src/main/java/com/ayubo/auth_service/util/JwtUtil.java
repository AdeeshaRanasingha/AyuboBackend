package com.ayubo.auth_service.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private Key key;
    private final long EXPIRATION_TIME = 1000 * 60 * 60 * 10; // 10 Hours

    @PostConstruct
    void init() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 characters for HS256");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role) // We embed the role so React knows who they are
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 2. Get the specific user email
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    // 3. Get the specific role (PATIENT, PROVIDER, etc.)
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // 4. Mathematically verify the token is authentic and hasn't expired
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false; // Token is fake, tampered with, or expired
        }
    }
}
