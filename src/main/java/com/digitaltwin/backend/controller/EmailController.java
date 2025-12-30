package com.digitaltwin.backend.controller;

import com.digitaltwin.backend.dto.JwtResponse;
import com.digitaltwin.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/verify/send")
    public ResponseEntity<?> sendOtp() {
        emailService.sendVerificationEmail();
        return ResponseEntity.ok("Verification email sent");
    }

    @PostMapping("/verify/confirm")
    public ResponseEntity<JwtResponse> confirmOtp(@RequestBody String otp) {
        return ResponseEntity.ok(emailService.confirmVerificationOtp(otp));
    }
}
