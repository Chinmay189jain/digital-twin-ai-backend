package com.digitaltwin.backend.controller;

import com.digitaltwin.backend.dto.JwtResponse;
import com.digitaltwin.backend.dto.OtpRequest;
import com.digitaltwin.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class EmailController {

    private final UserService userService;

    @PostMapping("/verify/send")
    public ResponseEntity<?> requestAccountVerificationOtp() {
        userService.sendAccountVerificationEmail();
        return ResponseEntity.ok("Email sent successfully");
    }

    @PostMapping("/verify/confirm")
    public ResponseEntity<JwtResponse> verifyAccountVerificationOtp(
            @Valid  @RequestBody OtpRequest.ConfirmOtpRequest request
    ) {
        return ResponseEntity.ok(userService.validateAccountVerificationOtp(request.getOtpCode()));
    }
}
