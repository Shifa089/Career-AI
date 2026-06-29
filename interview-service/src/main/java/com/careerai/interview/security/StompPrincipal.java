package com.careerai.interview.security;

import java.security.Principal;

/**
 * Minimal {@link Principal} carrying the authenticated user's id (as the principal name) for STOMP
 * messages, set by the {@link StompAuthChannelInterceptor} after validating the handshake JWT.
 */
public record StompPrincipal(String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }
}
