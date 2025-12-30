package com.digitaltwin.backend.service;

import com.digitaltwin.backend.dto.JwtResponse;
import com.digitaltwin.backend.model.OtpToken;
import com.digitaltwin.backend.model.User;
import com.digitaltwin.backend.repository.OtpTokenRepository;
import com.digitaltwin.backend.security.JwtService;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import static com.digitaltwin.backend.util.ConstantsTemplate.OTP_EMAIL_SUBJECT;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int OTP_RESEND_WAIT_SECONDS = 300; // 5 minutes

    @Autowired
    private UserService userService;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private OtpTokenRepository otpTokenRepository;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private JwtService jwtService;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public synchronized void sendVerificationEmail(String userEmail) {
        try {
            Optional<OtpToken> existingTokenOpt = otpTokenRepository.findByEmail(userEmail);
            if (existingTokenOpt.isPresent()) {
                LocalDateTime lastSentTime = existingTokenOpt.get().getCreatedAt();
                if (lastSentTime != null && lastSentTime.isAfter(LocalDateTime.now().minusSeconds(OTP_RESEND_WAIT_SECONDS))) {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please wait before requesting another OTP.");
                }
            }

            String otp = generateOtp();
            otpTokenRepository.save(OtpToken.builder()
                    .email(userEmail)
                    .otp(otp)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                    .build());

            sendOtpEmail(userEmail, otp);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error("❌ Error sending verification email", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send verification email");
        }
    }

    public void sendOtpEmail(String userEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(new InternetAddress(senderEmail, "Digital Twin AI"));
            helper.setTo(userEmail);
            helper.setSubject(OTP_EMAIL_SUBJECT);

            String html = loadAndPopulateHtmlTemplate(userEmail, otp);
            helper.setText(html, true);

            mailSender.send(message);
            logger.info("✅ OTP email sent to: {}", userEmail);

        } catch (Exception e) {
            logger.error("❌ Failed to send OTP email to: {}", userEmail, e);
        }
    }

    public boolean verifyOtp(String userEmail, String otp) {
        return otpTokenRepository.findByEmail(userEmail)
                .filter(token -> {
                    boolean matches = token.getOtp().equals(otp);
                    boolean notExpired = token.getExpiresAt().isAfter(LocalDateTime.now());
                    if (!matches) logger.warn("Invalid OTP for email {}", userEmail);
                    if (!notExpired) logger.warn("Expired OTP for email {}", userEmail);
                    return matches && notExpired;
                })
                .map(token -> {
                    otpTokenRepository.delete(token);
                    return true;
                })
                .orElse(false);
    }

    public void sendVerificationEmail() {
        String userEmail = userService.getCurrentUserEmail();
        User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isVerified()) {
            sendVerificationEmail(userEmail);
        }
    }

    public JwtResponse confirmVerificationOtp(String otp) {
        String userEmail = userService.getCurrentUserEmail();
        User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isVerified()) {
            String token = jwtService.generateToken(user);
            return new JwtResponse(token);
        }

        boolean isValid = verifyOtp(userEmail, otp);
        if (isValid) {
            user.setVerified(true);
            userService.saveUser(user);
        } else{
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired OTP");
        }

        String token = jwtService.generateToken(user);
        return new JwtResponse(token);
    }

    private String generateOtp() {
        int min = (int) Math.pow(10, OTP_LENGTH - 1);
        int max = (int) Math.pow(10, OTP_LENGTH) - 1;
        return String.valueOf(new Random().nextInt(max - min + 1) + min);
    }

    private String loadAndPopulateHtmlTemplate(String email, String otp) {
        try {
            Resource resource = resourceLoader.getResource("classpath:templates/otp-template.html");
            String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            return template
                    .replace("{{EMAIL}}", email)
                    .replace("{{OTP}}", otp)
                    .replace("{{EXPIRY_MINUTES}}", String.valueOf(OTP_EXPIRY_MINUTES))
                    .replace("{{EXPIRY_LABEL}}", "minute");

        } catch (Exception e) {
            logger.error("❌ Failed to load OTP HTML template", e);
            throw new RuntimeException("Could not generate email HTML");
        }
    }
}
