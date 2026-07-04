package com.careerai.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rate-limit tuning bound from {@code gateway.rate-limit.*}. Requests are limited per authenticated
 * user (keyed by user id) or, when no valid token is present, per client IP.
 */
@ConfigurationProperties(prefix = "gateway.rate-limit")
public class RateLimitProperties {

    /** Requests per minute allowed for an authenticated user. */
    private int authenticatedRpm = 100;

    /** Requests per minute allowed for an unauthenticated client (keyed by IP). */
    private int unauthenticatedRpm = 20;

    public int getAuthenticatedRpm() {
        return authenticatedRpm;
    }

    public void setAuthenticatedRpm(int authenticatedRpm) {
        this.authenticatedRpm = authenticatedRpm;
    }

    public int getUnauthenticatedRpm() {
        return unauthenticatedRpm;
    }

    public void setUnauthenticatedRpm(int unauthenticatedRpm) {
        this.unauthenticatedRpm = unauthenticatedRpm;
    }
}
