package com.digitaltwin.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TwinProfileResponse {

    private Map<Integer, String> profileAnswers; // User's answers to profile questions
    private String profileSummary;
}
