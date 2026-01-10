package com.digitaltwin.backend.controller;

import com.digitaltwin.backend.dto.JwtResponse;
import com.digitaltwin.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class EmailController {

    @Autowired
    private UserService userService;

    @PostMapping("/verify/send")
    public ResponseEntity<?> requestAccountVerificationOtp() {
        userService.sendAccountVerificationEmail();
        return ResponseEntity.ok("Email sent successfully");
    }

    @PostMapping("/verify/confirm")
    public ResponseEntity<JwtResponse> verifyAccountVerificationOtp(@RequestBody String otp) {
        return ResponseEntity.ok(userService.validateAccountVerificationOtp(otp));
    }
}
