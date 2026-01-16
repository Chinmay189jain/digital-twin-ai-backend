package com.digitaltwin.backend.controller;

import com.digitaltwin.backend.dto.LoginRequest;
import com.digitaltwin.backend.dto.UserRegistration;
import com.digitaltwin.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    // Register endpoint
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistration request) {
        return userService.registerUser(request);
    }

    // Login endpoint
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid  @RequestBody LoginRequest request) {
        return userService.loginUser(request);
    }
}
