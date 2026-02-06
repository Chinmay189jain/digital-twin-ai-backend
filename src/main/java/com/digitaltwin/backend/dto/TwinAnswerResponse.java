package com.digitaltwin.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TwinAnswerResponse {
    private String sessionId;
    private String question;
    private String aiResponse;
    private LocalDateTime timestamp;
}
