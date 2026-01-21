package com.digitaltwin.backend.controller;

import com.digitaltwin.backend.dto.ProfileAnswersRequest;
import com.digitaltwin.backend.dto.ProfileResponse;
import com.digitaltwin.backend.model.ProfileQuestion;
import com.digitaltwin.backend.service.TwinProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class ProfileController {

    @Autowired
    private TwinProfileService twinProfileService;

    // Get profile questions
    @GetMapping("/profile-questions")
    public ResponseEntity<List<ProfileQuestion>> getProfileQuestions() {
        return ResponseEntity.ok(twinProfileService.getProfileQuestions());
    }

    //Generate twin profile based on user answers
    @PostMapping("/generate-profile")
    public ResponseEntity<String> generateTwinProfile(@RequestBody ProfileAnswersRequest profileAnswersRequest) {
        return ResponseEntity.ok(twinProfileService.generateProfile(profileAnswersRequest.getProfileAnswers()));
    }

    //Generate twin profile based on user answers
    @PostMapping("/update-profile")
    public ResponseEntity<String> updateTwinProfile(@RequestBody ProfileAnswersRequest profileAnswersRequest) {
        return ResponseEntity.ok(twinProfileService.updateProfile(profileAnswersRequest.getProfileAnswers()));
    }

    // Get the digital twin profile
    @GetMapping("/get-profile")
    public ResponseEntity<ProfileResponse> getTwinProfile() {
        return ResponseEntity.ok(twinProfileService.getProfile());
    }
}
