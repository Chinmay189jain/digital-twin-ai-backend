package com.digitaltwin.backend.dto;

import lombok.Data;

public class OtpRequest {
    @Data
    public static class SendOtpRequest {
        private String email;
    }

    @Data
    public static class VerifyOtpRequest {
        private String email;
        private String otpCode;
    }

    @Data
    public static class ConfirmOtpRequest {
        private String otpCode;
    }
}
