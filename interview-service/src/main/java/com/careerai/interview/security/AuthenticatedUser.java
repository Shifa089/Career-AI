package com.careerai.interview.security;

import com.careerai.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * The identity of the caller, reconstructed from the gateway-forwarded {@code X-User-*} headers.
 * Stored as the authentication principal so controllers can resolve the current user.
 *
 * @param userId the authenticated user's id
 * @param email  the authenticated user's email
 */
public record AuthenticatedUser(UUID userId, String email) {

    /**
     * @return the {@link AuthenticatedUser} bound to the current security context
     * @throws UnauthorizedException if no authenticated user is present
     */
    public static AuthenticatedUser current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new UnauthorizedException("No authenticated user in the request context");
        }
        return user;
    }
}
