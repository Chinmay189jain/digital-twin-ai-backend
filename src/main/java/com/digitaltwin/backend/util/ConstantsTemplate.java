package com.digitaltwin.backend.util;

public class ConstantsTemplate {

    // --- Twin Profile Generation ---
    public static final String PROFILE_GENERATION_CONTEXT =
            "You are building a digital twin from user reflections.";

    public static final String PROFILE_PROMPT_PREFIX =
            "Here are my introspective answers:\n";

    public static final String PROFILE_PROMPT_SUFFIX =
            "\nSummarize my personality, values, fears, goals.";

    // --- Twin Identity + Q&A ---
    public static final String SYSTEM_TWIN_CONTEXT =
            "You are the AI-based digital twin of a real person. Act like them and answer questions based on their identity.";

    public static final String TWIN_USER_IDENTITY_PREFIX =
            "User profile summary:\n";

    public static final String USER_TWIN_INSTRUCTIONS =
            "User question:\n";

    // --- OTP mail messages ---
    public static final String OTP_EMAIL_SUBJECT = "🔐 Your OTP for Digital Twin AI";

    public static final String ACCOUNT_VERIFICATION_OTP_HEADER =
            "Email Verification OTP";

    public static final String ACCOUNT_VERIFICATION_OTP_MESSAGE =
            "Welcome to Digital Twin AI! Use the OTP below to verify your account.\n";

    public static final String PASSWORD_RESET_OTP_HEADER =
            "Password Reset OTP";

    public static final String PASSWORD_RESET_OTP_MESSAGE =
            "We received a request to reset your account password. Use the OTP below to continue.\n";
}
