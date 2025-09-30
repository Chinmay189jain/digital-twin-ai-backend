package com.digitaltwin.backend.service;

import com.digitaltwin.backend.dto.AIRequest;
import com.digitaltwin.backend.dto.AIResponse;
import com.digitaltwin.backend.model.TwinProfile;
import com.digitaltwin.backend.repository.TwinProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static com.digitaltwin.backend.util.ConstantsTemplate.*;
import java.util.List;

@Service
public class AIService {

    @Autowired
    private TwinProfileRepository twinProfileRepository;

    @Autowired
    private UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    private final String AI_MODEL;

    private final WebClient webClient;

    public AIService(@Value("${grok.api.url}") String apiUrl,
                     @Value("${grok.api.key}") String apiKey,
                     @Value("${grok.ai.model}") String aiModel) {

        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();

        this.AI_MODEL = aiModel;
    }

    public String generateTwinProfile(List<String> userAnswers) {

        String prompt = String.join(" ", userAnswers);

        // Create the AIRequest object with the system context and user prompt
        AIRequest request = new AIRequest(
            AI_MODEL,
                List.of(
                        new AIRequest.Message(ROLE_SYSTEM, PROFILE_GENERATION_CONTEXT),
                        new AIRequest.Message(ROLE_USER, PROFILE_PROMPT_PREFIX + prompt + PROFILE_PROMPT_SUFFIX)
                ),
            0.7
        );

        try {
            // Make the API call to Grok AI to generate the profile summary
            AIResponse response = webClient.post()
                    .uri("/openai/v1/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(new RuntimeException("GrokAI Error: " + errorBody)))
                    )
                    .bodyToMono(AIResponse.class)
                    .block();

            if(response!=null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                // Extract the generated profile summary from the response
                String profileSummary = response.getChoices().getFirst().getMessage().getContent();
                logger.info("Generated profile summary: {}", profileSummary);
                return profileSummary;
            } else{
                logger.error("Empty or null response from grok AI while generating profile");
                throw new RuntimeException("Failed to generate twin profile");
            }

        } catch (WebClientResponseException.TooManyRequests e) {
            throw new RuntimeException("Rate limit exceeded. Try again later." + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Error during Grok api call: " + e.getMessage());
        }
    }

    public String respondAsTwin(String userQuestion) {

        TwinProfile profile = twinProfileRepository.findByUserId(userService.getCurrentUserEmail())
                .orElseThrow(() -> new RuntimeException("Twin profile not found"));

        // Create the AIRequest object with the system context and user question
        AIRequest request = new AIRequest(
                AI_MODEL,
                List.of(
                        new AIRequest.Message(ROLE_SYSTEM, SYSTEM_TWIN_CONTEXT),
                        new AIRequest.Message(ROLE_USER, TWIN_USER_IDENTITY_PREFIX + profile.getProfileSummary()),
                        new AIRequest.Message(ROLE_USER, USER_TWIN_INSTRUCTIONS + userQuestion)
                ),
                0.7
        );

        try {
            // Make the API call to Grok AI to get the response for user question
            AIResponse response = webClient.post()
                    .uri("/openai/v1/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(new RuntimeException("GrokAI Error: " + errorBody)))
                    )
                    .bodyToMono(AIResponse.class)
                    .block();

            if(response!=null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                // Extract the generated answer from the response
                String answer = response.getChoices().getFirst().getMessage().getContent();
                logger.info("Generated answer from user question: {}", answer);
                return answer;
            } else{
                logger.error("Empty or null response from grok AI while generating answer");
                throw new RuntimeException("Failed to generate answer for user question");
            }

        } catch (WebClientResponseException.TooManyRequests e) {
            throw new RuntimeException("Rate limit exceeded. Try again later.");
        } catch (Exception e) {
            throw new RuntimeException("Error during Grok api call: " + e.getMessage());
        }
    }
}
