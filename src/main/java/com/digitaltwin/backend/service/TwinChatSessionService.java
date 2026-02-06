package com.digitaltwin.backend.service;

import com.digitaltwin.backend.dto.ChatSessionListItem;
import com.digitaltwin.backend.model.TwinChatSession;
import com.digitaltwin.backend.repository.TwinChatSessionRepository;
import com.mongodb.lang.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TwinChatSessionService {

    private final TwinChatSessionRepository twinChatSessionRepository;
    private final UserService userService;

    public TwinChatSession createSession(String email, String userQuestion) {
        try {
            // Create a new chat session
            TwinChatSession session = TwinChatSession.builder()
                    .userId(email)
                    .title(deriveTitleFrom(userQuestion))
                    .messageCount(1)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // Save the updated session
            twinChatSessionRepository.save(session);

            return session;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create chat session", e);
        }
    }

    public TwinChatSession getSession(String email, String sessionId) {
        try{
            // Retrieve the chat session by ID
            TwinChatSession session = twinChatSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found"));

            // Verify that the session belongs to the requesting user
            if (!Objects.equals(session.getUserId(), email)) {
                throw new SecurityException("Not allowed to access this session");
            }

            session.setMessageCount(session.getMessageCount()+1);
            session.setUpdatedAt(LocalDateTime.now());

            // Save the updated session
            twinChatSessionRepository.save(session);

            return session;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve chat session", e);
        }
    }

    // Retrieve all chat sessions for the current user, optionally filtered by a search query
    public List<ChatSessionListItem> getAllSessions(@Nullable String searchQuery) {
        try{
            final List<TwinChatSession> sessions;

            // If searchQuery is blank, retrieve all sessions; otherwise, perform a search
            if(isBlank(searchQuery)){
                sessions = twinChatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userService.getCurrentUserEmail());
            } else {
                sessions = twinChatSessionRepository.searchByTitle(userService.getCurrentUserEmail(), searchQuery);
            }

            return sessions.stream()
                    .map(session -> new ChatSessionListItem(
                            session.getId(),
                            session.getTitle(),
                            session.getMessageCount(),
                            session.getUpdatedAt()
                    )).toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve all chat sessions", e);
        }
    }

    // Validate that the session exists and belongs to the current user
    public void validateSession(String sessionId) {
        TwinChatSession session = twinChatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!Objects.equals(session.getUserId(), userService.getCurrentUserEmail())) {
            throw new SecurityException("Not allowed to access this session");
        }
    }

    // Delete a chat session by its ID
    public void deleteSession(String sessionId) {
        try {
            twinChatSessionRepository.deleteById(sessionId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete chat session", e);
        }
    }

    public long deleteAllSessionsByUserId(String userId) {
        try {
            return twinChatSessionRepository.deleteByUserId(userId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete chat sessions by user ID", e);
        }
    }

    // Utility method to check if a string is blank (null or empty after trimming)
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    // Derives a title from the question, truncating if necessary
    private String deriveTitleFrom(String question) {
        String ques = question.strip();
        if (ques.length() > 60) {
            ques = ques.substring(0, 60) + "…";
        }
        return ques;
    }
}
