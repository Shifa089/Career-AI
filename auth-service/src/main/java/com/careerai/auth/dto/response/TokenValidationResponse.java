package com.careerai.auth.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Result of validating an access token; consumed by the API gateway.
 */
public record TokenValidationResponse(
        UUID userId,
        String email,
        List<String> roles,
        boolean valid
) {
}
