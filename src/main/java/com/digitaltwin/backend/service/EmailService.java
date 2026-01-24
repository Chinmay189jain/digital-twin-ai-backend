package com.digitaltwin.backend.service;

import com.digitaltwin.backend.model.OtpToken;
import com.digitaltwin.backend.repository.OtpTokenRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import static com.digitaltwin.backend.util.ConstantsTemplate.*;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${otp.length}")
    private int otpLength;

    @Value("${otp.expiry-minutes}")
    private int otpExpiryMinutes;

    @Value("${otp.resend-wait-seconds}")
    private int otpResendWaitSeconds;

    @Value("${spring.mail.username}")
    private String senderEmail;

    private final JavaMailSender mailSender;

    private final OtpTokenRepository otpTokenRepository;

    private final ResourceLoader resourceLoader;

    private final PasswordEncoder passwordEncoder;

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

            sendEmail(userEmail, otp, purpose);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error("❌ Error sending verification email", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send verification email");
        }
    }

    @Async
    public void sendEmail(String userEmail, String otp, OtpPurpose purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(new InternetAddress(senderEmail, "Digital Twin AI"));
            helper.setTo(userEmail);
            helper.setSubject(OTP_EMAIL_SUBJECT);

            String html = loadAndPopulateHtmlTemplate(userEmail, otp, purpose);
            helper.setText(html, true);

            mailSender.send(message);
            logger.info("✅ OTP email sent to: {}", userEmail);

        } catch (Exception e) {
            logger.error("❌ Failed to send OTP email to: {}", userEmail, e);
            otpTokenRepository.deleteByEmailAndPurpose(userEmail, purpose);
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

    private String loadAndPopulateHtmlTemplate(String email, String otp, OtpPurpose purpose) {
        try {
            Resource resource = resourceLoader.getResource("classpath:templates/otp-template.html");
            String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            String HEADING = purpose == OtpPurpose.PASSWORD_RESET ? PASSWORD_RESET_OTP_HEADER : ACCOUNT_VERIFICATION_OTP_HEADER;
            String MESSAGE = purpose == OtpPurpose.PASSWORD_RESET ? PASSWORD_RESET_OTP_MESSAGE : ACCOUNT_VERIFICATION_OTP_MESSAGE;

            return template
                    .replace("{{EMAIL}}", email)
                    .replace("{{OTP}}", otp)
                    .replace("{{EXPIRY_MINUTES}}", String.valueOf(otpExpiryMinutes))
                    .replace("{{EXPIRY_LABEL}}", "minute")
                    .replace("{{HEADING}}", HEADING)
                    .replace("{{MESSAGE}}", MESSAGE);

        } catch (Exception e) {
            logger.error("❌ Failed to load OTP HTML template", e);
            throw new RuntimeException("Could not generate email HTML");
        }
    }
}
