package com.digitaltwin.backend.service;

import com.digitaltwin.backend.dto.ProfileAnswersRequest;
import com.digitaltwin.backend.dto.ProfileResponse;
import com.digitaltwin.backend.model.ProfileQuestion;
import com.digitaltwin.backend.model.TwinProfile;
import com.digitaltwin.backend.repository.ProfileQuestionRepository;
import com.digitaltwin.backend.repository.TwinProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TwinProfileService {

    @Autowired
    private TwinProfileRepository twinProfileRepository;

    @Autowired
    private ProfileQuestionRepository profileQuestionRepository;

    @Autowired
    private AIService aiService;

    @Autowired
    private UserService userService;

    public List<ProfileQuestion> getProfileQuestions() {
        try{
            return profileQuestionRepository.findAllByOrderByIdAsc();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve profile questions: " + e.getMessage(), e);
        }
    }

    public String generateProfile(Map<Integer, String> profileAnswersMap) {
        try{
            // Fetch all profile questions from the repository
            List<ProfileQuestion> profileQuestion = profileQuestionRepository.findAllByOrderByIdAsc();

            // Combine user answers with question prefixes
            List<String> combinedUserAnswer = createPromptOfProfileAnswers(profileQuestion, profileAnswersMap);

            // Generate the profile summary using AIService
            String profileSummary = aiService.generateTwinProfile(combinedUserAnswer);

            // Create a new TwinProfile object with the generated summary
            TwinProfile profile = TwinProfile.builder()
                    .userId(userService.getCurrentUserEmail()) // Replace with actual user ID
                    .profileAnswers(profileAnswersMap)
                    .profileSummary(profileSummary)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();

            // Save the generated profile to the database
            twinProfileRepository.save(profile);

            return  profileSummary;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate twin profile: " + e.getMessage(), e);
        }
    }

    public String updateProfile(Map<Integer, String> profileAnswersMap) {
        try{

            TwinProfile profile = twinProfileRepository.findByUserId(userService.getCurrentUserEmail())
                    .orElseThrow(() -> new RuntimeException("Twin profile not found"));

            // If the answers are the same, skip api call and return the existing summary
            if(Objects.equals(profileAnswersMap, profile.getProfileAnswers())){
                return profile.getProfileSummary();
            }

            // Fetch all profile questions from the repository
            List<ProfileQuestion> profileQuestion = profileQuestionRepository.findAllByOrderByIdAsc();

            // Combine user answers with question prefixes
            List<String> combinedUserAnswer = createPromptOfProfileAnswers(profileQuestion, profileAnswersMap);

            // Generate the profile summary using AIService
            String profileSummary = aiService.generateTwinProfile(combinedUserAnswer);

            // Create a new TwinProfile object with the generated summary
            profile.setProfileAnswers(profileAnswersMap);
            profile.setProfileSummary(profileSummary);

            // Save the generated profile to the database
            twinProfileRepository.save(profile);

            return  profileSummary;
        } catch (Exception e) {
            throw new RuntimeException("Failed to update twin profile: " + e.getMessage(), e);
        }
    }

    public ProfileResponse getProfile() {
        try{
            TwinProfile profile = twinProfileRepository.findByUserId(userService.getCurrentUserEmail())
                    .orElseThrow(() -> new RuntimeException("Twin profile not found"));

            return new ProfileResponse(profile.getProfileAnswers(), profile.getProfileSummary());
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve twin profile: " + e.getMessage(), e);
        }
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
}
