package com.digitaltwin.backend.service;

import java.util.List;

public interface AIService {
    String generateTwinProfile(List<String> userAnswers);
    String respondAsTwin(String userQuestion);
}
