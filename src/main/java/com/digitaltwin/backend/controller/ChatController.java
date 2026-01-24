package com.digitaltwin.backend.controller;

import com.digitaltwin.backend.dto.ChatHistoryResponse;
import com.digitaltwin.backend.dto.ChatSessionListItem;
import com.digitaltwin.backend.dto.TwinAnswerResponse;
import com.digitaltwin.backend.dto.TwinQuestionRequest;
import com.digitaltwin.backend.service.TwinChatService;
import com.digitaltwin.backend.service.TwinChatSessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/twin")
@RequiredArgsConstructor
@Validated // Needed for validating @PathVariable / @RequestParam constraints
public class ChatController {

    private final TwinChatService twinChatService;

    private final TwinChatSessionService twinChatSessionService;

    // Endpoint to get a response from the AI based on user input
    @PostMapping("/chat")
    public ResponseEntity<TwinAnswerResponse> getChatResponse(@Valid  @RequestBody TwinQuestionRequest twinQuestionRequest) {
        TwinAnswerResponse answer = twinChatService.getResponse(twinQuestionRequest.getSessionId(), twinQuestionRequest.getUserQuestion());
        return ResponseEntity.ok(answer);
    }

    // Endpoint to get chat history with optional search query
    @GetMapping("/chat/sessions")
    public ResponseEntity<List<ChatSessionListItem>> getAllSessions(@RequestParam (required = false) String searchQuery) {
        return ResponseEntity.ok(twinChatSessionService.getAllSessions(searchQuery));
    }

    // Endpoint to get chat history for a specific session with pagination
    @GetMapping("/chat/{sessionId}")
    public ResponseEntity<ChatHistoryResponse> getChatHistory(
            @PathVariable @NotBlank(message = "sessionId is required") String sessionId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be >= 0") int page,
            @RequestParam(defaultValue = "30")
            @Min(value = 1, message = "size must be >= 1")
            @Max(value = 200, message = "size must be <= 200") int size
    ) {
        ChatHistoryResponse response = twinChatService.getChatHistory(sessionId, page, size);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("chat/session/{sessionId}" )
    public ResponseEntity<String> deleteChatSession(
            @PathVariable @NotBlank(message = "sessionId is required") String sessionId
    ) {
        twinChatService.deleteChatSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
