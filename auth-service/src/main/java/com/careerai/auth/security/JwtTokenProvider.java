package com.careerai.auth.security;

import com.careerai.auth.domain.RefreshToken;
import com.careerai.auth.domain.entity.User;
import com.careerai.auth.service.RefreshTokenService;
import com.careerai.common.security.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Issues and inspects HS256 JWTs. Low-level signing/parsing is delegated to the
 * shared {@link JwtUtil}; this component layers on token-type semantics and the
 * Redis-backed refresh-token store.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    public static final String TYPE_ACCESS = "ACCESS";
    public static final String TYPE_REFRESH = "REFRESH";

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE = "type";

    private final RefreshTokenService refreshTokenService;

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    private JwtUtil jwtUtil;

    @PostConstruct
    void init() {
        this.jwtUtil = new JwtUtil(secret);
    }

    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TYPE, TYPE_ACCESS);
        claims.put(CLAIM_ROLES, extractRoles(userDetails));
        if (userDetails instanceof User user && user.getId() != null) {
            claims.put(CLAIM_USER_ID, user.getId().toString());
        }
        return jwtUtil.generateToken(userDetails.getUsername(), claims, accessTokenExpiryMs);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TYPE, TYPE_REFRESH);
        String token = jwtUtil.generateToken(userDetails.getUsername(), claims, refreshTokenExpiryMs);

        String userId = (userDetails instanceof User user && user.getId() != null)
                ? user.getId().toString() : null;
        RefreshToken record = RefreshToken.builder()
                .token(token)
                .userId(userId)
                .email(userDetails.getUsername())
                .expiresAt(Instant.now().plusMillis(refreshTokenExpiryMs))
                .revoked(false)
                .build();
        refreshTokenService.saveRefreshToken(record);
        return token;
    }

    public boolean validateToken(String token) {
        return jwtUtil.isValid(token);
    }

    public String extractEmail(String token) {
        return jwtUtil.extractSubject(token);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return jwtUtil.extractClaim(token, claimsResolver);
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = jwtUtil.extractClaim(token, Claims::getExpiration);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public String getTokenType(String token) {
        return jwtUtil.extractClaim(token, c -> c.get(CLAIM_TYPE, String.class));
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Object roles = jwtUtil.extractClaim(token, c -> c.get(CLAIM_ROLES));
        return roles instanceof List<?> list ? (List<String>) list : List.of();
    }

    public void invalidateRefreshToken(String token) {
        refreshTokenService.revokeToken(token);
    }

    public long getAccessTokenExpiryMs() {
        return accessTokenExpiryMs;
    }

    private List<String> extractRoles(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
