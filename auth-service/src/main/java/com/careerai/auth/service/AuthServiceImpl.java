package com.careerai.auth.service;

import com.careerai.auth.domain.RefreshToken;
import com.careerai.auth.domain.entity.Role;
import com.careerai.auth.domain.entity.User;
import com.careerai.auth.domain.enums.AuthProvider;
import com.careerai.auth.domain.enums.RoleName;
import com.careerai.auth.dto.request.LoginRequest;
import com.careerai.auth.dto.request.RefreshTokenRequest;
import com.careerai.auth.dto.request.RegisterRequest;
import com.careerai.auth.dto.request.ResetPasswordRequest;
import com.careerai.auth.dto.request.UpdateProfileRequest;
import com.careerai.auth.dto.response.AuthResponse;
import com.careerai.auth.dto.response.TokenValidationResponse;
import com.careerai.auth.dto.response.UserResponse;
import com.careerai.auth.exception.AuthException;
import com.careerai.auth.exception.TokenException;
import com.careerai.auth.exception.UserAlreadyExistsException;
import com.careerai.auth.mapper.UserMapper;
import com.careerai.auth.repository.RoleRepository;
import com.careerai.auth.repository.UserRepository;
import com.careerai.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implements the local-credential authentication flows plus token validation for
 * the gateway. Refresh tokens are rotated on every refresh.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String OTP_KEY_PREFIX = "pwd-reset:";
    private static final Duration OTP_TTL = Duration.ofMinutes(15);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("An account with email " + request.email() + " already exists");
        }
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER is not seeded"));

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .provider(AuthProvider.LOCAL)
                .emailVerified(false)
                .enabled(true)
                .build();
        user.addRole(userRole);
        User saved = userRepository.save(user);

        emailService.sendVerificationEmail(saved.getEmail(), UUID.randomUUID().toString());

        return issueTokens(saved);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = (User) authentication.getPrincipal();

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String oldToken = request.refreshToken();

        if (!tokenProvider.validateToken(oldToken)
                || !JwtTokenProvider.TYPE_REFRESH.equals(safeTokenType(oldToken))) {
            throw new TokenException("Invalid refresh token");
        }
        if (refreshTokenService.isRevoked(oldToken)) {
            throw new TokenException("Refresh token has been revoked or expired");
        }

        String email = tokenProvider.extractEmail(oldToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new TokenException("Refresh token subject no longer exists"));

        // Rotation: invalidate the presented token before minting a new pair.
        refreshTokenService.revokeToken(oldToken);
        return issueTokens(user);
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revokeToken(refreshToken);
        }
    }

    @Override
    public void forgotPassword(String email) {
        // Do not reveal whether the account exists; only send mail when it does.
        userRepository.findByEmail(email).ifPresent(user -> {
            String otp = generateOtp();
            redisTemplate.opsForValue().set(OTP_KEY_PREFIX + email, otp, OTP_TTL);
            emailService.sendPasswordResetOtp(email, otp);
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String key = OTP_KEY_PREFIX + request.email();
        String storedOtp = redisTemplate.opsForValue().get(key);
        if (storedOtp == null || !storedOtp.equals(request.otp())) {
            throw new AuthException("Invalid or expired reset code", HttpStatus.BAD_REQUEST);
        }
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthException("Account not found", HttpStatus.NOT_FOUND));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        redisTemplate.delete(key);
    }

    @Override
    public TokenValidationResponse validateToken(String token) {
        if (token == null || !tokenProvider.validateToken(token)
                || !JwtTokenProvider.TYPE_ACCESS.equals(safeTokenType(token))) {
            return new TokenValidationResponse(null, null, java.util.List.of(), false);
        }
        String email = tokenProvider.extractEmail(token);
        return userRepository.findByEmail(email)
                .map(user -> new TokenValidationResponse(
                        user.getId(), user.getEmail(), tokenProvider.getRoles(token), true))
                .orElseGet(() -> new TokenValidationResponse(null, null, java.util.List.of(), false));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Account not found", HttpStatus.NOT_FOUND));
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Account not found", HttpStatus.NOT_FOUND));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setProfilePictureUrl(request.profilePictureUrl());
        return userMapper.toUserResponse(userRepository.save(user));
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);
        UserResponse userResponse = userMapper.toUserResponse(user);
        return AuthResponse.of(accessToken, refreshToken, tokenProvider.getAccessTokenExpiryMs(), userResponse);
    }

    private String safeTokenType(String token) {
        try {
            return tokenProvider.getTokenType(token);
        } catch (Exception e) {
            return null;
        }
    }

    private String generateOtp() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
