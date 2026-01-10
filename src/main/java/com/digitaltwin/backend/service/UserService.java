package com.digitaltwin.backend.service;

import com.digitaltwin.backend.dto.JwtResponse;
import com.digitaltwin.backend.dto.LoginRequest;
import com.digitaltwin.backend.dto.UserRegistration;
import com.digitaltwin.backend.model.User;
import com.digitaltwin.backend.repository.UserRepository;
import com.digitaltwin.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private EmailService emailService;

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    // This method finds a user by their email
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void deleteByEmail(String email){
        userRepository.deleteByEmail(email);
    }

    // This method retrieves the current user's email from the security context
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName(); // This gives the email from JWT
    }

    public ResponseEntity<?> registerUser(UserRegistration request){
        // Check if email is already registered
        if (findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("User with this email already exists");
        }

        // Create new user with encoded password
        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .isVerified(false)
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new JwtResponse(token));
    }

    public ResponseEntity<?> loginUser(LoginRequest request){
        try {
            // Authenticate user credentials
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid email or password, Enter correct credentials");
        }

        User user = findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + request.getEmail()));

        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new JwtResponse(token));
    }

    // Send account verification email to the current user
    public void sendAccountVerificationEmail() {
        String userEmail = getCurrentUserEmail();
        User user = findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!user.isVerified()) {
            emailService.generateOtp(userEmail, OtpPurpose.ACCOUNT_VERIFICATION);
        }
    }

    public JwtResponse validateAccountVerificationOtp(String otp) {
        String userEmail = getCurrentUserEmail();
        User user = findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (user.isVerified()) {
            String token = jwtService.generateToken(user);
            return new JwtResponse(token);
        }

        boolean isValid = emailService.validateOtp(userEmail, otp, OtpPurpose.ACCOUNT_VERIFICATION);
        if (isValid) {
            user.setVerified(true);
            saveUser(user);
        } else{
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired OTP");
        }

        String token = jwtService.generateToken(user);
        return new JwtResponse(token);
    }

}
