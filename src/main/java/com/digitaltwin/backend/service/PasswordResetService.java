package com.digitaltwin.backend.service;

import com.digitaltwin.backend.dto.JwtResponse;
import com.digitaltwin.backend.model.User;
import com.digitaltwin.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordResetService {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void sendPasswordResetEmail(String userEmail) {
        User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        emailService.generateOtp(user.getEmail(), OtpPurpose.PASSWORD_RESET);
    }

    public JwtResponse validatePasswordResetOtp(String userEmail, String otpCode) {
        User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        boolean isValid = emailService.validateOtp(user.getEmail(), otpCode, OtpPurpose.PASSWORD_RESET);

        if (!isValid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired OTP");
        }

        String token = jwtService.generatePasswordResetToken(user);
        return new JwtResponse(token);
    }

    // Reset password using reset token for unauthenticated user
    public void resetForgottenPassword(String resetToken, String newPassword, String confirmPassword) {
        if (resetToken == null || resetToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing reset token");
        }

        if (newPassword == null || newPassword.isBlank() || confirmPassword == null || confirmPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be empty");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }

        if (!jwtService.isTokenValid(resetToken) || !jwtService.isPasswordResetToken(resetToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired reset token");
        }

        String userEmail = jwtService.extractEmail(resetToken);
        User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userService.saveUser(user);
    }

    // Reset password for authenticated user
    public void resetAuthenticatedUserPassword(String currentPassword, String newPassword, String confirmPassword) {
        String userEmail = userService.getCurrentUserEmail();
        User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (currentPassword == null || currentPassword.isBlank()
                || newPassword == null || newPassword.isBlank()
                || confirmPassword == null || confirmPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All password fields are required");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userService.saveUser(user);
    }
}
