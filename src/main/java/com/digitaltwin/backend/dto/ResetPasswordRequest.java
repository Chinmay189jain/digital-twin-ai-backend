package com.digitaltwin.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResetPasswordRequest {
    @Data
    public static class ForgotPasswordRequest {

        @NotBlank(message = "New password is required")
        @Size(min = 6, max = 72, message = "New password must be between 8 and 72 characters")
        private String newPassword;

        @NotBlank(message = "Confirm password is required")
        @Size(min = 6, max = 72, message = "Confirm password must be between 8 and 72 characters")
        private String confirmPassword;
    }

    @Data
    public static class AuthenticatedPasswordRequest {

        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 6, max = 72, message = "New password must be between 8 and 72 characters")
        private String newPassword;

        @NotBlank(message = "Confirm password is required")
        @Size(min = 6, max = 72, message = "Confirm password must be between 8 and 72 characters")
        private String confirmPassword;
    }
}
