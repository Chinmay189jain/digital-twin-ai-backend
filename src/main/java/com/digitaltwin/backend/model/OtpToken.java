package com.digitaltwin.backend.model;

import com.digitaltwin.backend.service.OtpPurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "otp_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndex(name = "email_purpose_idx", def = "{'email': 1, 'purpose': 1}", unique = true)
public class OtpToken {

    @Id
    private String id;
    private String email;
    private String otpHash;
    private OtpPurpose purpose;
    private LocalDateTime expiresAt;
    private LocalDateTime lastSentAt;
}
