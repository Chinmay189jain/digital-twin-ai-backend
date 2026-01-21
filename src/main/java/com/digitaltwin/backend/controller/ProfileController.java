package com.digitaltwin.backend.controller;

import com.digitaltwin.backend.dto.TwinProfileRequest;
import com.digitaltwin.backend.dto.TwinProfileResponse;
import com.digitaltwin.backend.model.ProfileQuestion;
import com.digitaltwin.backend.service.TwinProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

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
    public ResponseEntity<String> generateTwinProfile(@Valid @RequestBody TwinProfileRequest twinProfileRequest) {
        return ResponseEntity.ok(twinProfileService.generateProfile(twinProfileRequest));
    }

    //Generate twin profile based on user answers
    @PostMapping("/update-profile")
    public ResponseEntity<TwinProfileResponse> updateTwinProfile(@Valid @RequestBody TwinProfileRequest twinProfileRequest) {
        return ResponseEntity.ok(twinProfileService.updateProfile(twinProfileRequest));
    }

    // Get the digital twin profile
    @GetMapping("/get-profile")
    public ResponseEntity<TwinProfileResponse> getTwinProfile() {
        return ResponseEntity.ok(twinProfileService.getProfile());
    }
}
