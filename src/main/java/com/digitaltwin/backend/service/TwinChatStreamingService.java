package com.digitaltwin.backend.service;

import com.digitaltwin.backend.AIConfig.AiFailure;
import com.digitaltwin.backend.AIConfig.AiFailureMapper;
import com.digitaltwin.backend.AIConfig.AiStreamGuard;
import com.digitaltwin.backend.dto.TwinWebSocketEvent;
import com.digitaltwin.backend.dto.TwinWebSocketQuestionRequest;
import com.digitaltwin.backend.model.TwinChat;
import com.digitaltwin.backend.model.TwinChatSession;
import com.digitaltwin.backend.repository.TwinChatRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TwinChatStreamingService {

    private static final Logger logger = LoggerFactory.getLogger(TwinChatStreamingService.class);

    private final TwinChatRepository twinChatRepository;
    private final TwinChatSessionService twinChatSessionService;
    private final AIService aiService;
    private final SimpMessagingTemplate messagingTemplate;

    private final AiStreamGuard aiStreamGuard;
    private final AiFailureMapper aiFailureMapper;

    // in-flight streams by clientMessageId (for cancel)
    private final Map<String, Disposable> inflight = new ConcurrentHashMap<>();

    public void streamAnswerToCurrentUser(String email, TwinWebSocketQuestionRequest req) {

        if (req.getUserQuestion() == null || req.getUserQuestion().trim().isEmpty()) {
            send(email, TwinWebSocketEvent.builder()
                    .type(TwinEventType.ERROR)
                    .clientMessageId(req.getClientMessageId())
                    .error("Question cannot be empty")
                    .timestamp(System.currentTimeMillis())
                    .build());
            return;
        }

        // get or create session
        TwinChatSession session = (req.getSessionId() == null || req.getSessionId().trim().isEmpty())
                ? twinChatSessionService.createSession(email, req.getUserQuestion())
                : twinChatSessionService.getSession(email, req.getSessionId());

        if (req.getSessionId() == null || req.getSessionId().trim().isEmpty()) {
            send(email, TwinWebSocketEvent.builder()
                    .type(TwinEventType.SESSION_CREATED)
                    .sessionId(session.getId())
                    .clientMessageId(req.getClientMessageId())
                    .timestamp(System.currentTimeMillis())
                    .build());
        }

        send(email, TwinWebSocketEvent.builder()
                .type(TwinEventType.START)
                .sessionId(session.getId())
                .clientMessageId(req.getClientMessageId())
                .timestamp(System.currentTimeMillis())
                .build());

        // stream from AI
        StringBuilder full = new StringBuilder();

        // Make a fresh AI stream per request
        Flux<String> raw = Flux.defer(() -> aiService.streamRespondAsTwin(email, req.getUserQuestion()));

        // Apply protection
        Flux<String> protectedStream = aiStreamGuard.protect(raw)
                .bufferTimeout(1, Duration.ofMillis(20)) // reduce websocket spam
                .filter(list -> !list.isEmpty())
                .map(list -> String.join("", list));


        Disposable d = protectedStream.subscribe(
                chunk -> {
                    full.append(chunk);
                    send(email, TwinWebSocketEvent.builder()
                            .type(TwinEventType.DELTA)
                            .sessionId(session.getId())
                            .clientMessageId(req.getClientMessageId())
                            .delta(chunk)
                            .timestamp(System.currentTimeMillis())
                            .build());
                },
                ex -> {
                    AiFailure failure = aiFailureMapper.toFailure(ex);

                    send(email, TwinWebSocketEvent.builder()
                            .type(TwinEventType.ERROR)
                            .sessionId(session.getId())
                            .clientMessageId(req.getClientMessageId())
                            .errorCode(failure.getErrorCode().name())
                            .error(failure.getUserMessage())
                            .timestamp(System.currentTimeMillis())
                            .build());

                    inflight.remove(req.getClientMessageId());
                },
                () -> {
                    // persist final answer
                    TwinChat chat = TwinChat.builder()
                            .userId(email)
                            .sessionId(session.getId())
                            .question(req.getUserQuestion())
                            .aiResponse(full.toString())
                            .timestamp(java.time.LocalDateTime.now())
                            .build();
                    twinChatRepository.save(chat);

                    send(email, TwinWebSocketEvent.builder()
                            .type(TwinEventType.DONE)
                            .sessionId(session.getId())
                            .clientMessageId(req.getClientMessageId())
                            .fullText(full.toString())
                            .timestamp(System.currentTimeMillis())
                            .build());

                    inflight.remove(req.getClientMessageId());
                }
        );

        inflight.put(req.getClientMessageId(), d);
    }

    public void cancel(String clientMessageId) {
        Disposable d = inflight.remove(clientMessageId);
        if (d != null) d.dispose();
    }

    private void send(String email, TwinWebSocketEvent event) {
        // private per-user stream
        //logger.info("WS OUT -> user={} dest=/queue/twin.events type={}", email, event.getType());
        messagingTemplate.convertAndSendToUser(email, "/queue/twin.events", event);
    }
}

