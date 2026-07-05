package com.careerai.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Local-account login payload.
 */
public record LoginRequest(

        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
