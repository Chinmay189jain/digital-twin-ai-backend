package com.digitaltwin.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResetPasswordRequest {
    @Data
    public static class ForgotPasswordRequest {
        private String newPassword;
        private String confirmPassword;
    }

    @Data
    public static class AuthenticatedPasswordRequest {
        private String currentPassword;
        private String newPassword;
        private String confirmPassword;
    }
}
