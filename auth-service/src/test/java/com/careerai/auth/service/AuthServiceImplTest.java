package com.careerai.auth.service;

import com.careerai.auth.domain.entity.Role;
import com.careerai.auth.domain.entity.User;
import com.careerai.auth.domain.enums.AuthProvider;
import com.careerai.auth.domain.enums.RoleName;
import com.careerai.auth.dto.request.LoginRequest;
import com.careerai.auth.dto.request.RefreshTokenRequest;
import com.careerai.auth.dto.request.RegisterRequest;
import com.careerai.auth.dto.response.AuthResponse;
import com.careerai.auth.dto.response.UserResponse;
import com.careerai.auth.exception.TokenException;
import com.careerai.auth.exception.UserAlreadyExistsException;
import com.careerai.auth.mapper.UserMapper;
import com.careerai.auth.repository.RoleRepository;
import com.careerai.auth.repository.UserRepository;
import com.careerai.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private EmailService emailService;
    @Mock private UserMapper userMapper;
    @Mock private StringRedisTemplate redisTemplate;

    @InjectMocks private AuthServiceImpl authService;

    private static final String EMAIL = "jane@example.com";
    private static final String PASSWORD = "Str0ng!Pass";

    private User sampleUser() {
        Role role = Role.builder().id(UUID.randomUUID()).name(RoleName.ROLE_USER).build();
        return User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .password("hashed")
                .firstName("Jane")
                .lastName("Doe")
                .provider(AuthProvider.LOCAL)
                .roles(Set.of(role))
                .build();
    }

    private void stubTokenIssuance(User user) {
        when(tokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");
        when(tokenProvider.getAccessTokenExpiryMs()).thenReturn(900_000L);
        when(userMapper.toUserResponse(any())).thenReturn(new UserResponse(
                user.getId(), EMAIL, "Jane", "Doe", null,
                List.of("ROLE_USER"), false, AuthProvider.LOCAL, LocalDateTime.now()));
    }

    @Test
    void register_succeeds_andSendsVerificationEmail() {
        RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, "Jane", "Doe");
        User saved = sampleUser();

        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_USER))
                .thenReturn(Optional.of(Role.builder().id(UUID.randomUUID()).name(RoleName.ROLE_USER).build()));
        when(passwordEncoder.encode(PASSWORD)).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        stubTokenIssuance(saved);

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo(EMAIL);
        verify(emailService).sendVerificationEmail(eq(EMAIL), anyString());
    }

    @Test
    void register_duplicateEmail_throwsUserAlreadyExists() {
        RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, "Jane", "Doe");
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void login_succeeds_updatesLastLogin_andReturnsTokens() {
        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);
        User user = sampleUser();
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.save(any(User.class))).thenReturn(user);
        stubTokenIssuance(user);

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(user.getLastLoginAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void login_badCredentials_propagates() {
        LoginRequest request = new LoginRequest(EMAIL, "wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refreshToken_valid_rotatesAndReturnsNewTokens() {
        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh");
        User user = sampleUser();

        when(tokenProvider.validateToken("old-refresh")).thenReturn(true);
        when(tokenProvider.getTokenType("old-refresh")).thenReturn(JwtTokenProvider.TYPE_REFRESH);
        when(refreshTokenService.isRevoked("old-refresh")).thenReturn(false);
        when(tokenProvider.extractEmail("old-refresh")).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        stubTokenIssuance(user);

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(refreshTokenService, times(1)).revokeToken("old-refresh");
    }

    @Test
    void refreshToken_revoked_throwsTokenException() {
        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh");
        when(tokenProvider.validateToken("old-refresh")).thenReturn(true);
        when(tokenProvider.getTokenType("old-refresh")).thenReturn(JwtTokenProvider.TYPE_REFRESH);
        when(refreshTokenService.isRevoked("old-refresh")).thenReturn(true);

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(TokenException.class);

        verify(refreshTokenService, never()).revokeToken(anyString());
    }

    @Test
    void logout_revokesRefreshToken() {
        authService.logout("some-refresh");
        verify(refreshTokenService).revokeToken("some-refresh");
    }
}
