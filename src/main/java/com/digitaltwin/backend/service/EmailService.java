package com.digitaltwin.backend.service;

import com.digitaltwin.backend.repository.OtpTokenRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import java.nio.charset.StandardCharsets;
import static com.digitaltwin.backend.util.ConstantsTemplate.*;
import static com.digitaltwin.backend.util.ConstantsTemplate.ACCOUNT_VERIFICATION_OTP_MESSAGE;
import static com.digitaltwin.backend.util.ConstantsTemplate.PASSWORD_RESET_OTP_MESSAGE;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${otp.expiry-minutes}")
    private int otpExpiryMinutes;

    private final JavaMailSender mailSender;
    private final ResourceLoader resourceLoader;
    private final OtpTokenRepository otpTokenRepository;

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
