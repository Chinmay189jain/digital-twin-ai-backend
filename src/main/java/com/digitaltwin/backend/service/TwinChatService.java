package com.digitaltwin.backend.service;

import com.digitaltwin.backend.dto.ChatHistoryResponse;
import com.digitaltwin.backend.dto.TwinAnswerResponse;
import com.digitaltwin.backend.model.TwinChat;
import com.digitaltwin.backend.repository.TwinChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TwinChatService {

    private final TwinChatRepository twinChatRepository;
    private final TwinChatSessionService twinChatSessionService;
    private final UserService userService;

    // Retrieve chat history for a specific session
    public ChatHistoryResponse getChatHistory(String sessionId, int page, int size) {
        try {
            final String email = userService.getCurrentUserEmail();

            if (isBlank(sessionId)) throw new IllegalArgumentException("Session ID cannot be empty");

            // Validate session ownership and existence
            twinChatSessionService.validateSession(sessionId);

            Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());

            // Retrieve all chat interactions for the given session and user
            Slice<TwinChat> chats = twinChatRepository.findByUserIdAndSessionIdOrderByTimestampAsc(email, sessionId, pageable);

            List<TwinAnswerResponse> messages = chats.getContent()
                    .stream()
                    .map(chat -> new TwinAnswerResponse(
                            chat.getSessionId(),
                            chat.getQuestion(),
                            chat.getAiResponse(),
                            chat.getTimestamp()))
                    .collect(Collectors.toList());

            Collections.reverse(messages); // Reverse to have most recent messages first

            return new ChatHistoryResponse(messages, chats.hasNext());

        } catch (Exception e) {
            throw new RuntimeException("Error in getChatHistory: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteChatSession(String sessionId) {
        try {
            final String email = userService.getCurrentUserEmail();

            if (isBlank(sessionId)) throw new IllegalArgumentException("Session ID cannot be empty");

            // Validate session ownership and existence
            twinChatSessionService.validateSession(sessionId);

            // Delete all chat interactions for the given session and user
            twinChatRepository.deleteByUserIdAndSessionId(email, sessionId);

            //delete the session itself
            twinChatSessionService.deleteSession(sessionId);

        } catch (Exception e) {
            throw new RuntimeException("Error in deleting chat history: " + e.getMessage());
        }
    }

    public long deleteAllChatsByUserId(String userId) {
        try {
            return twinChatRepository.deleteByUserId(userId);
        } catch (Exception e) {
            throw new RuntimeException("Error in deleting chat by userId: " + e.getMessage());
        }
    }

    // Utility method to check if a string is blank (null or empty after trimming)
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
