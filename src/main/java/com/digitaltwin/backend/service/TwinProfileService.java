package com.digitaltwin.backend.service;

import com.digitaltwin.backend.dto.TwinProfileRequest;
import com.digitaltwin.backend.dto.TwinProfileResponse;
import com.digitaltwin.backend.model.ProfileQuestion;
import com.digitaltwin.backend.model.TwinProfile;
import com.digitaltwin.backend.repository.ProfileQuestionRepository;
import com.digitaltwin.backend.repository.TwinProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TwinProfileService {

    private final TwinProfileRepository twinProfileRepository;
    private final ProfileQuestionRepository profileQuestionRepository;
    private final AIService aiService;
    private final UserService userService;
    private final TwinProfileCacheService twinProfileCacheService;

    public List<ProfileQuestion> getProfileQuestions() {
        try{
            return profileQuestionRepository.findAllByOrderByIdAsc();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve profile questions: " + e.getMessage(), e);
        }
    }

    public String generateProfile(TwinProfileRequest twinProfileRequest) {
        try{
            final String email = userService.getCurrentUserEmail();

            // Fetch all profile questions from the repository
            List<ProfileQuestion> profileQuestion = profileQuestionRepository.findAllByOrderByIdAsc();

            // Combine user answers with question prefixes
            List<String> combinedUserAnswer = createPromptOfProfileAnswers(profileQuestion, twinProfileRequest.getProfileAnswers());

            // Generate the profile summary using GroqAIService
            String profileSummary = aiService.generateTwinProfile(combinedUserAnswer);

            // Create a new TwinProfile object with the generated summary
            TwinProfile profile = TwinProfile.builder()
                    .userId(email) // Replace with actual user ID
                    .profileAnswers(twinProfileRequest.getProfileAnswers())
                    .profileSummary(profileSummary)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();

            // Save the generated profile to the database
            twinProfileRepository.save(profile);

            // remove old cache if any, so that next read will get updated value
            twinProfileCacheService.evictProfile(email);

            return  profileSummary;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate twin profile: " + e.getMessage(), e);
        }
    }

    public TwinProfileResponse updateProfile(TwinProfileRequest twinProfileRequest) {
        try{

            final String email = userService.getCurrentUserEmail();

            TwinProfile profile = twinProfileRepository.findByUserId(email)
                    .orElseThrow(() -> new RuntimeException("Twin profile not found"));

            // If the answers are the same, skip api call and return the existing summary
            if(Objects.equals(twinProfileRequest.getProfileAnswers(), profile.getProfileAnswers())){
                return new TwinProfileResponse(profile.getProfileAnswers(), profile.getProfileSummary());
            }

            // Fetch all profile questions from the repository
            List<ProfileQuestion> profileQuestion = profileQuestionRepository.findAllByOrderByIdAsc();

            // Combine user answers with question prefixes
            List<String> combinedUserAnswer = createPromptOfProfileAnswers(profileQuestion, twinProfileRequest.getProfileAnswers());

            // Generate the profile summary using GroqAIService
            String profileSummary = aiService.generateTwinProfile(combinedUserAnswer);

            // Create a new TwinProfile object with the generated summary
            profile.setProfileAnswers(twinProfileRequest.getProfileAnswers());
            profile.setProfileSummary(profileSummary);

            // Save the generated profile to the database
            twinProfileRepository.save(profile);

            // remove old cache if any, so that next read will get updated value
            twinProfileCacheService.evictProfile(email);

            return new TwinProfileResponse(profile.getProfileAnswers(), profile.getProfileSummary());
        } catch (Exception e) {
            throw new RuntimeException("Failed to update twin profile: " + e.getMessage(), e);
        }
    }

    public TwinProfileResponse getProfile() {
        String email = userService.getCurrentUserEmail();
        return twinProfileCacheService.getProfile(email);
    }

    public List<String> createPromptOfProfileAnswers(List<ProfileQuestion> profileQuestionList, Map<Integer, String> profileAnswersMap) {
        try {
            if (profileQuestionList == null || profileQuestionList.isEmpty() || profileAnswersMap == null || profileAnswersMap.isEmpty()) {
                return List.of();
            }

            // Result size can’t exceed either list or map size
            List<String> combinedUserAnswer = new ArrayList<>(Math.min(profileQuestionList.size(), profileAnswersMap.size()));

            for (ProfileQuestion question : profileQuestionList) {
                if (question == null || question.getId() == null) continue;
                String answer = profileAnswersMap.get(question.getId());

                if (answer == null || answer.isEmpty()) continue; // skip blank answers or null answers
                String prefix = question.getPrefix() == null ? "" : question.getPrefix().trim();

                combinedUserAnswer.add(prefix +" "+ answer);
            }
            return combinedUserAnswer;
        } catch (Exception e) {
            throw new RuntimeException("Error creating prompt from profile answers: " + e.getMessage(), e);
        }
    }

    public long deleteAllProfilesByUserId(String userId) {
        try {
            long deleted = twinProfileRepository.deleteByUserId(userId);
            twinProfileCacheService.evictProfile(userId);
            return deleted;
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete twin profile by user ID: " + e.getMessage(), e);
        }
    }
}
