package com.digitaltwin.backend.service;

import com.digitaltwin.backend.model.TwinProfile;
import com.digitaltwin.backend.repository.TwinProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import static com.digitaltwin.backend.util.ConstantsTemplate.*;

import java.util.List;

@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiAIService implements AIService{

    private static final Logger logger = LoggerFactory.getLogger(GeminiAIService.class);

    @Autowired
    private TwinProfileRepository twinProfileRepository;

    @Autowired
    private UserService userService;

    private final ChatClient chatClient;

    public GeminiAIService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String generateTwinProfile(List<String> userAnswers) {
        String prompt = String.join(" ", userAnswers);
        try {

            return chatClient.prompt()
                    .system(s -> s.text(PROFILE_GENERATION_CONTEXT))
                    .user(PROFILE_PROMPT_PREFIX + prompt + PROFILE_PROMPT_SUFFIX)
                    .call()
                    .content();

        } catch (Exception e) {
            logger.error("Error while generating twin profile: {}", e.getMessage());
            throw new RuntimeException("Failed to generate twin profile" + e.getMessage(), e);
        }
    }

    @Override
    public String respondAsTwin(String userQuestion) {
        TwinProfile profile = twinProfileRepository
                .findByUserId(userService.getCurrentUserEmail())
                .orElseThrow(() -> new RuntimeException("Twin profile not found"));

        try {
            String answer =
                    chatClient.prompt()
                            .system(s -> s.text(SYSTEM_TWIN_CONTEXT))
                            .user(TWIN_USER_IDENTITY_PREFIX + profile.getProfileSummary())
                            .user(USER_TWIN_INSTRUCTIONS + userQuestion)
                            .call()
                            .content();

            return answer;

        } catch (Exception e) {
            logger.error("Error while generating response from AI service: {}", e.getMessage());
            throw new RuntimeException("Failed to get response from AI service: " + e.getMessage(), e);
        }
    }
}
