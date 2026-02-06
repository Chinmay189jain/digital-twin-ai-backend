package com.digitaltwin.backend.security;

import com.digitaltwin.backend.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-in-ms}")
    private long expirationInMs;

    @Value("${jwt.password-reset-expiration-in-ms}")
    private long passwordResetExpirationInMs;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Generate token
    public String generateToken(User user) {
        return buildToken(
                Map.of(
                        "username", user.getName(),
                        "verified", user.isVerified(),
                        "purpose", TokenPurpose.AUTH
                ),
                user.getEmail(),
                expirationInMs
        );
    }

    // Generate password reset token
    public String generatePasswordResetToken(User user) {
        return buildToken(
                Map.of(
                        "username", user.getName(),
                        "purpose", TokenPurpose.PASSWORD_RESET
                ),
                user.getEmail(),
                passwordResetExpirationInMs
        );
    }

    // Build token
    public String buildToken(Map<String, Object> claims, String subject, long expirationInMs) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationInMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Validate token
    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Extract email
    public String extractEmail(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    // Extract purpose
    public String extractPurpose(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return String.valueOf(claims.get("purpose"));
    }

    public boolean isAuthToken(String token) {
        return Objects.equals(extractPurpose(token), TokenPurpose.AUTH.name());
    }

    public boolean isPasswordResetToken(String token) {
        return Objects.equals(extractPurpose(token), TokenPurpose.PASSWORD_RESET.name());
    }
}