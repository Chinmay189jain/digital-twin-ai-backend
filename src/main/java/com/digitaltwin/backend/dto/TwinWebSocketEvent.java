package com.digitaltwin.backend.dto;

import com.digitaltwin.backend.service.TwinEventType;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TwinWebSocketEvent {

    @NotNull
    private TwinEventType type;

    @Size(max = 100, message = "sessionId must be <= 100 characters")
    private String sessionId;

    @NotBlank(message = "clientMessageId is required")
    @Size(max = 100, message = "clientMessageId must be <= 100 characters")
    private String clientMessageId;

    @Size(max = 5000, message = "delta must be <= 5000 characters")
    private String delta;

    @Size(max = 50000, message = "fullText must be <= 50000 characters")
    private String fullText;

    @Size(max = 2000, message = "error must be <= 2000 characters")
    private String error;

    @Size(max = 50, message = "errorCode must be <= 50 characters")
    private String errorCode;

    @NotNull(message = "timestamp is required")
    @Positive(message = "timestamp must be a positive number")
    private Long timestamp;

    @AssertTrue(message = "Invalid payload for event type")
    public boolean isValidForType() {
        if (type == null) return false;

        boolean hasDelta = delta != null && !delta.isBlank();
        boolean hasFull  = fullText != null && !fullText.isBlank();
        boolean hasError = error != null && !error.isBlank();

        return switch (type) {
            case DELTA -> hasDelta && !hasFull && !hasError;
            case DONE -> hasFull && !hasDelta && !hasError;
            case ERROR -> hasError && !hasDelta && !hasFull;
            case START, SESSION_CREATED -> !hasDelta && !hasFull && !hasError;
        };
    }
}

