package com.digitaltwin.backend.service;

import com.digitaltwin.backend.model.OtpToken;
import com.digitaltwin.backend.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    @Value("${otp.length}")
    private int otpLength;

    @Value("${otp.expiry-minutes}")
    private int otpExpiryMinutes;

    @Value("${otp.resend-wait-seconds}")
    private int otpResendWaitSeconds;
    ;
    private final OtpTokenRepository otpTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public void generateOtp(String userEmail, OtpPurpose purpose) {
        try {
            Optional<OtpToken> existingTokenOpt = otpTokenRepository.findByEmailAndPurpose(userEmail, purpose);
            if (existingTokenOpt.isPresent()) {
                LocalDateTime lastSentTime = existingTokenOpt.get().getLastSentAt();
                if (lastSentTime != null && lastSentTime.isAfter(LocalDateTime.now().minusSeconds(otpResendWaitSeconds))) {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please wait before requesting another OTP.");
                }
            }

            String otp = generateOtp();

            LocalDateTime now = LocalDateTime.now();
            otpTokenRepository.deleteByEmailAndPurpose(userEmail, purpose);
            otpTokenRepository.save(OtpToken.builder()
                    .email(userEmail)
                    .otpHash(passwordEncoder.encode(otp))
                    .purpose(purpose)
                    .lastSentAt(now)
                    .expiresAt(now.plusMinutes(otpExpiryMinutes))
                    .build());

            emailService.sendEmail(userEmail, otp, purpose);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error("❌ Error sending verification email", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send verification email");
        }
    }

    public boolean validateOtp(String userEmail, String otp, OtpPurpose purpose) {
        return otpTokenRepository.findByEmailAndPurpose(userEmail, purpose)
                .filter(token -> {
                    boolean matches = passwordEncoder.matches(otp, token.getOtpHash());
                    boolean notExpired = token.getExpiresAt().isAfter(LocalDateTime.now());
                    if (!matches) logger.warn("Invalid OTP for email {}", userEmail);
                    if (!notExpired) logger.warn("Expired OTP for email {}", userEmail);
                    return matches && notExpired;
                })
                .map(token -> {
                    otpTokenRepository.deleteByEmailAndPurpose(userEmail, purpose);
                    return true;
                })
                .orElse(false);
    }

    private String generateOtp() {
        int min = (int) Math.pow(10, otpLength - 1);
        int max = (int) Math.pow(10, otpLength) - 1;
        return String.valueOf(new SecureRandom().nextInt(max - min + 1) + min);
    }
}
