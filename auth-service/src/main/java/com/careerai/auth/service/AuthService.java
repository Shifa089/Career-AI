package com.careerai.auth.service;

import com.careerai.auth.dto.request.LoginRequest;
import com.careerai.auth.dto.request.RefreshTokenRequest;
import com.careerai.auth.dto.request.RegisterRequest;
import com.careerai.auth.dto.request.ResetPasswordRequest;
import com.careerai.auth.dto.request.UpdateProfileRequest;
import com.careerai.auth.dto.response.AuthResponse;
import com.careerai.auth.dto.response.TokenValidationResponse;
import com.careerai.auth.dto.response.UserResponse;

/**
 * Core authentication operations: registration, login, token lifecycle, and
 * password recovery.
 */
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String refreshToken);

    void forgotPassword(String email);

    void resetPassword(ResetPasswordRequest request);

    TokenValidationResponse validateToken(String token);

    UserResponse getProfile(String email);

    UserResponse updateProfile(String email, UpdateProfileRequest request);
}
