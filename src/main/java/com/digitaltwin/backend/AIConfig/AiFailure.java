package com.digitaltwin.backend.AIConfig;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiFailure {
    private AiErrorCode errorCode;
    private String userMessage;
}
