package com.careerai.auth.service;

import com.careerai.auth.domain.RefreshToken;

import java.util.Optional;

/**
 * Redis-backed store for refresh tokens, supporting rotation and revocation.
 */
public interface RefreshTokenService {

    /** Persist a refresh token with a TTL derived from its {@code expiresAt}. */
    void saveRefreshToken(RefreshToken refreshToken);

    /** Look up a stored refresh token by its value. */
    Optional<RefreshToken> findByToken(String token);

    /** Delete a refresh token (rotation/logout). */
    void revokeToken(String token);

    /** True when the token is absent from Redis or explicitly marked revoked. */
    boolean isRevoked(String token);
}
