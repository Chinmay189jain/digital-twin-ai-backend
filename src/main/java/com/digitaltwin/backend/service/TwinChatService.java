package com.digitaltwin.backend.service;

import com.digitaltwin.backend.dto.TwinAnswerResponse;
import com.digitaltwin.backend.model.TwinChat;
import com.digitaltwin.backend.model.TwinChatSession;
import com.digitaltwin.backend.repository.TwinChatRepository;
import com.mongodb.lang.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TwinChatService {

    @Autowired
    private TwinChatRepository twinChatRepository;

    @Autowired
    private TwinChatSessionService twinChatSessionService;

    @Autowired
    private AIService aiService;

    @Autowired
    private UserService userService;

    // Process user question and return AI response, managing chat sessions
    public TwinAnswerResponse getResponse(@Nullable String sessionId, String userQuestion) {
        try {

            final String email = userService.getCurrentUserEmail();

            if (isBlank(userQuestion)) throw new IllegalArgumentException("Question cannot be empty");

            // Get AI response
            String aiResponse = aiService.respondAsTwin(userQuestion);

            // Create or retrieve the chat session based on the provided sessionId
            TwinChatSession session;
            if(isBlank(sessionId)) {
                session = twinChatSessionService.createSession(email, userQuestion);
            } else {
                session = twinChatSessionService.getSession(email, sessionId);
            }

            TwinChat chat = TwinChat.builder()
                    .userId(email)
                    .sessionId(session.getId())
                    .question(userQuestion)
                    .aiResponse(aiResponse)
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            // Save the chat interaction to the database
            twinChatRepository.save(chat);

            return new TwinAnswerResponse(session.getId(), chat.getQuestion(), chat.getAiResponse(), chat.getTimestamp());
        } catch (Exception e) {
            throw new RuntimeException("Error in getResponse: " + e.getMessage());
        }
    }

    // Retrieve chat history for a specific session
    public List<TwinAnswerResponse> getChatHistory(String sessionId) {
        try {
            final String email = userService.getCurrentUserEmail();

            if (isBlank(sessionId)) throw new IllegalArgumentException("Session ID cannot be empty");

            // Validate session ownership and existence
            twinChatSessionService.validateSession(sessionId);

            // Retrieve all chat interactions for the given session and user
            List<TwinChat> chats = twinChatRepository.findByUserIdAndSessionIdOrderByTimestampAsc(email, sessionId);

            return chats.stream()
                    .map(chat -> new TwinAnswerResponse(
                            chat.getSessionId(),
                            chat.getQuestion(),
                            chat.getAiResponse(),
                            chat.getTimestamp()))
                    .toList();
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

    // Utility method to check if a string is blank (null or empty after trimming)
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
