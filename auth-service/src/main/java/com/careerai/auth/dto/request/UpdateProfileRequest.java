package com.careerai.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Editable fields of a user's own profile.
 */
public record UpdateProfileRequest(

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        String profilePictureUrl
) {
}
