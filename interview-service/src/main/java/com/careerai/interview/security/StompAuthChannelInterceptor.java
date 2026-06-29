package com.careerai.interview.security;

import com.careerai.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Authenticates the STOMP {@code CONNECT} frame by validating the bearer JWT in its
 * {@code Authorization} header and binding a {@link StompPrincipal} (the user id) to the session,
 * so downstream {@code @MessageMapping} handlers can resolve the caller.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader("Authorization");
            if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)) {
                String token = authorization.substring(BEARER_PREFIX.length());
                if (jwtUtil.isValid(token)) {
                    accessor.setUser(new StompPrincipal(jwtUtil.extractSubject(token)));
                } else {
                    log.debug("Rejected STOMP CONNECT with invalid token");
                }
            }
        }
        return message;
    }
}
