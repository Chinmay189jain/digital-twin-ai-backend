package com.digitaltwin.backend.service;

import com.digitaltwin.backend.repository.TwinProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import static com.digitaltwin.backend.util.ConstantsTemplate.*;
import java.util.List;

@Service
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    private final TwinProfileRepository twinProfileRepository;
    private final ChatClient chatClient;

    public AIService(
            ChatClient.Builder chatClientBuilder,
            TwinProfileRepository twinProfileRepository
    ) {
        this.chatClient = chatClientBuilder.build();
        this.twinProfileRepository = twinProfileRepository;
    }

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

    public Flux<String> streamRespondAsTwin(String userEmail, String userQuestion) {
        return Mono.fromSupplier(() ->
                        twinProfileRepository.findByUserId(userEmail)
                                .orElseThrow(() -> new IllegalStateException("Twin profile not found"))
                )
                .flatMapMany(profile ->
                        chatClient.prompt()
                                .system(s -> s.text(SYSTEM_TWIN_CONTEXT))
                                .user(TWIN_USER_IDENTITY_PREFIX + profile.getProfileSummary())
                                .user(USER_TWIN_INSTRUCTIONS + userQuestion)
                                .stream()
                                .content() // Flux<String>
                )
                .doOnError(ex -> logger.error("Gemini stream failed user={}", userEmail, ex));
    }
}
