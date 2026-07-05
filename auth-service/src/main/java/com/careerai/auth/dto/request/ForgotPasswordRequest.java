package com.careerai.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to start the password-reset flow; an OTP is emailed to this address.
 */
public record ForgotPasswordRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email
) {
}
