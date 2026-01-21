package com.digitaltwin.backend.service;

import com.digitaltwin.backend.dto.AIRequest;
import com.digitaltwin.backend.dto.AIResponse;
import com.digitaltwin.backend.model.TwinProfile;
import com.digitaltwin.backend.repository.TwinProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static com.digitaltwin.backend.util.ConstantsTemplate.*;

@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "groq")
public class GroqAIService implements AIService {

    private static final Logger log = LoggerFactory.getLogger(GroqAIService.class);

    private static final String CHAT_COMPLETIONS_PATH = "/openai/v1/chat/completions";
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final TwinProfileRepository twinProfileRepository;
    private final UserService userService;

    private final WebClient webClient;
    private final String model;

    public GroqAIService(
            TwinProfileRepository twinProfileRepository,
            UserService userService,
            @Value("${groq.api.url}") String apiUrl,
            @Value("${groq.api.key}") String apiKey,
            @Value("${groq.ai.model}") String model
    ) {
        this.twinProfileRepository = twinProfileRepository;
        this.userService = userService;
        this.model = model;

        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    @Override
    public String generateTwinProfile(List<String> userAnswers) {
        String prompt = String.join(" ", userAnswers);

        AIRequest request = new AIRequest(
                model,
                List.of(
                        new AIRequest.Message(ROLE_SYSTEM, PROFILE_GENERATION_CONTEXT),
                        new AIRequest.Message(ROLE_USER, PROFILE_PROMPT_PREFIX + prompt + PROFILE_PROMPT_SUFFIX)
                ),
                DEFAULT_TEMPERATURE
        );

        AIResponse response = callGroq(request);
        String summary = extractContentOrThrow(response, "twin profile");

        return summary;
    }

    @Override
    public String respondAsTwin(String userQuestion) {
        String email = userService.getCurrentUserEmail();

        TwinProfile profile = twinProfileRepository.findByUserId(email)
                .orElseThrow(() -> new RuntimeException("Twin profile not found"));

        AIRequest request = new AIRequest(
                model,
                List.of(
                        new AIRequest.Message(ROLE_SYSTEM, SYSTEM_TWIN_CONTEXT),
                        new AIRequest.Message(ROLE_USER, TWIN_USER_IDENTITY_PREFIX + profile.getProfileSummary()),
                        new AIRequest.Message(ROLE_USER, USER_TWIN_INSTRUCTIONS + userQuestion)
                ),
                DEFAULT_TEMPERATURE
        );

        AIResponse response = callGroq(request);
        String answer = extractContentOrThrow(response, "answer");

        return answer;
    }

    private AIResponse callGroq(AIRequest request) {
        return webClient.post()
                .uri(CHAT_COMPLETIONS_PATH)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AIResponse.class)
                .timeout(REQUEST_TIMEOUT)
                .onErrorMap(WebClientResponseException.class, this::mapWebClientError)
                .block();
    }

    private RuntimeException mapWebClientError(WebClientResponseException e) {
        if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            // if Groq returns retry-after header, you can read it here
            return new RuntimeException("Rate limit exceeded. Please retry after some time.");
        }
        String body = safeBody(e);
        return new RuntimeException("Groq API error " + e.getRawStatusCode() + ": " + body);
    }

    private String safeBody(WebClientResponseException e) {
        try {
            String body = e.getResponseBodyAsString();
            if (body == null) return "";
            return body.length() > 500 ? body.substring(0, 500) + "..." : body;
        } catch (Exception ex) {
            return "";
        }
    }

    private String extractContentOrThrow(AIResponse response, String operationName) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()
                || response.getChoices().getFirst() == null
                || response.getChoices().getFirst().getMessage() == null) {
            throw new RuntimeException("Empty/null response from Groq while generating " + operationName);
        }

        String content = response.getChoices().getFirst().getMessage().getContent();
        if (content == null || content.isBlank()) {
            throw new RuntimeException("Blank content from Groq while generating " + operationName);
        }

        return content.trim();
    }
}
