package com.digitaltwin.backend.AIConfig;

import com.digitaltwin.backend.security.CustomerUserDetailsService;
import com.digitaltwin.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);

    private final JwtService jwtService;
    private final CustomerUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        StompCommand cmd = accessor.getCommand();

        // IMPORTANT: ignore internal/heartbeat/close frames
        if (cmd == null || cmd == StompCommand.DISCONNECT) {
            return message;
        }

        logger.info(
                "STOMP {} dest={} session={} user={}",
                cmd,
                accessor.getDestination(),
                accessor.getSessionId(),
                accessor.getUser() != null ? accessor.getUser().getName() : "null"
        );

        // CONNECT: authenticate + store into session
        if (cmd == StompCommand.CONNECT) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new AccessDeniedException("Invalid or missing token");
            }

            String token = authHeader.substring(7);
            String userEmail = jwtService.extractEmail(token);

            if (userEmail == null || !jwtService.isTokenValid(token) || !jwtService.isAuthToken(token)) {
                throw new AccessDeniedException("Invalid or missing token");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );

            accessor.setUser(authentication);

            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put("AUTH", authentication);
            }

            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        // For SUBSCRIBE/SEND: restore user from session if missing
        boolean updatedUser = false;
        if ((cmd == StompCommand.SEND || cmd == StompCommand.SUBSCRIBE)
                && accessor.getUser() == null
                && accessor.getSessionAttributes() != null) {

            Object stored = accessor.getSessionAttributes().get("AUTH");
            if (stored instanceof UsernamePasswordAuthenticationToken auth) {
                accessor.setUser(auth);
                updatedUser = true;
            }
        }

        // Enforce auth only for SEND/SUBSCRIBE
        if ((cmd == StompCommand.SEND || cmd == StompCommand.SUBSCRIBE)
                && accessor.getUser() == null) {
            throw new AccessDeniedException("Unauthenticated STOMP message");
        }

        return updatedUser
                ? MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders())
                : message;
    }

}
