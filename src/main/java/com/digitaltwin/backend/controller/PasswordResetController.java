package com.digitaltwin.backend.controller;

import com.digitaltwin.backend.dto.ResetPasswordRequest;
import com.digitaltwin.backend.dto.JwtResponse;
import com.digitaltwin.backend.dto.OtpRequest;
import com.digitaltwin.backend.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/password/change")
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping("/forgot/mail/send")
    public ResponseEntity<?> requestPasswordResetOtp(@Valid  @RequestBody OtpRequest.SendOtpRequest request) {
        passwordResetService.sendPasswordResetEmail(request.getEmail());
        return ResponseEntity.ok("Email sent successfully");
    }

    @PostMapping("/forgot/mail/verify")
    public ResponseEntity<JwtResponse> verifyPasswordResetOtp(@Valid @RequestBody OtpRequest.VerifyOtpRequest request) {
        return ResponseEntity.ok(passwordResetService.validatePasswordResetOtp(request.getEmail(), request.getOtpCode()));
    }

    @PostMapping("/forgot/reset")
    public ResponseEntity<?> resetForgottenPassword(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody ResetPasswordRequest.ForgotPasswordRequest request
    ) {
        String token = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }
        passwordResetService.resetForgottenPassword(token, request.getNewPassword(), request.getConfirmPassword());
        return ResponseEntity.ok("Password updated successfully");
    }

    @PatchMapping("/authenticated/reset")
    public ResponseEntity<?> resetAuthenticatedUserPassword(
            @Valid @RequestBody ResetPasswordRequest.AuthenticatedPasswordRequest request
    ) {
        passwordResetService.resetAuthenticatedUserPassword(request.getCurrentPassword(), request.getNewPassword(), request.getConfirmPassword());
        return ResponseEntity.ok("Password updated successfully");
    }
}
