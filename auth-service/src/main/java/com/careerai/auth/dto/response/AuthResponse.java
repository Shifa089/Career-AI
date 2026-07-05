package com.careerai.auth.dto.response;

import lombok.Builder;

/**
 * Issued credential pair plus the authenticated user's profile.
 */
@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    /** Convenience builder that defaults {@code tokenType} to {@code "Bearer"}. */
    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(user)
                .build();
    }
}
