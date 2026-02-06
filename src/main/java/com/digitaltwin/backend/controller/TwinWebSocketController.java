package com.digitaltwin.backend.controller;

import com.digitaltwin.backend.dto.TwinWebSocketQuestionRequest;
import com.digitaltwin.backend.service.TwinChatStreamingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class TwinWebSocketController {

    private final TwinChatStreamingService streamingService;
    private static final Logger logger = LoggerFactory.getLogger(TwinWebSocketController.class);

    @MessageMapping("/twin.chat")
    public void chat(TwinWebSocketQuestionRequest req, Principal principal) {

        if (principal == null) {
            // user is not authenticated
            throw new RuntimeException("Unauthorized WebSocket message, principal is null");
        }

        String email = principal.getName();  // this will be your email/username
        streamingService.streamAnswerToCurrentUser(email, req);
    }

    @MessageMapping("/twin.cancel")
    public void cancel(String clientMessageId) {
        streamingService.cancel(clientMessageId);
    }
}
