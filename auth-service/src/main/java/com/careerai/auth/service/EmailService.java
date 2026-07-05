package com.careerai.auth.service;

/**
 * Transactional email delivery for the auth flows.
 */
public interface EmailService {

    /** Send an account-verification email containing the verification token/link. */
    void sendVerificationEmail(String email, String token);

    /** Send a password-reset email containing the one-time passcode. */
    void sendPasswordResetOtp(String email, String otp);
}
