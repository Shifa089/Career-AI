package com.careerai.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends HTML transactional emails via the configured {@link JavaMailSender}.
 * In dev this targets MailHog (localhost:1025); see application-dev.yml.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    // A dedicated From address — NOT spring.mail.username, which is intentionally blank for
    // auth-less dev SMTP (Mailpit). Deriving From from an empty username yields "Illegal address".
    @Value("${app.mail.from:no-reply@careerai.com}")
    private String fromAddress;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void sendVerificationEmail(String email, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto">
                  <h2>Welcome to CareerAI</h2>
                  <p>Please confirm your email address to activate your account.</p>
                  <p><a href="%s" style="background:#4f46e5;color:#fff;padding:12px 20px;\
                text-decoration:none;border-radius:6px;display:inline-block">Verify email</a></p>
                  <p>Or paste this link into your browser:<br><a href="%s">%s</a></p>
                </div>
                """.formatted(link, link, link);
        send(email, "Verify your CareerAI account", html);
    }

    @Override
    public void sendPasswordResetOtp(String email, String otp) {
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto">
                  <h2>Password reset</h2>
                  <p>Use the following one-time code to reset your password. It expires in 15 minutes.</p>
                  <p style="font-size:28px;font-weight:bold;letter-spacing:6px">%s</p>
                  <p>If you did not request this, you can safely ignore this email.</p>
                </div>
                """.formatted(otp);
        send(email, "Your CareerAI password reset code", html);
    }

    private void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.debug("Sent '{}' email to {}", subject, to);
        } catch (MessagingException | MailException e) {
            // Email delivery must never break the auth flow; log and continue.
            log.error("Failed to send '{}' email to {}: {}", subject, to, e.getMessage());
        }
    }
}
