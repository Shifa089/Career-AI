package com.careerai.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Token-refresh payload carrying a previously issued refresh JWT.
 */
public record RefreshTokenRequest(

        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
