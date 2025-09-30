package com.digitaltwin.backend.util;

public class ConstantsTemplate {

    // --- Role Constants ---
    public static final String ROLE_USER = "user";
    public static final String ROLE_SYSTEM = "system";

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
            "Here’s who I am:\n";

    public static final String USER_TWIN_INSTRUCTIONS =
            "Now answer this question like I would:\n";

}
