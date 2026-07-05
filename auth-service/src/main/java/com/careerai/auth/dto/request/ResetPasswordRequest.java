package com.careerai.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Completes the password-reset flow using the emailed OTP.
 */
public record ResetPasswordRequest(

        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "OTP is required")
        String otp,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "Password must contain an uppercase letter, a digit, and a special character")
        String newPassword
) {
}
