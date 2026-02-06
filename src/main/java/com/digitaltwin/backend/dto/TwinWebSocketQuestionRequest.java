package com.digitaltwin.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TwinWebSocketQuestionRequest {

    @Size(max = 100, message = "sessionId must be at most 100 characters")
    private String sessionId;

    @NotBlank(message = "userQuestion is required")
    @Size(min = 1, max = 2000, message = "userQuestion must be between 1 and 2000 characters")
    private String userQuestion;

    @NotBlank(message = "clientMessageId is required")
    @Size(max = 100, message = "clientMessageId must be at most 100 characters")
    private String clientMessageId;
}
