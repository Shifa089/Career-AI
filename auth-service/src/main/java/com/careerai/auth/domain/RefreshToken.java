package com.careerai.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Refresh token record persisted in Redis as JSON (not a JPA entity).
 * Keyed under {@code refresh:{token}} with a TTL equal to the refresh-token expiry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    private String token;
    private String userId;
    private String email;
    private Instant expiresAt;
    private boolean revoked;
}
