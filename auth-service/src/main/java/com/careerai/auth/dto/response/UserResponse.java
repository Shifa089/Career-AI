package com.careerai.auth.dto.response;

import com.careerai.auth.domain.enums.AuthProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Public-facing view of a {@link com.careerai.auth.domain.entity.User}.
 */
public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String profilePictureUrl,
        List<String> roles,
        boolean emailVerified,
        AuthProvider provider,
        LocalDateTime createdAt
) {
}
