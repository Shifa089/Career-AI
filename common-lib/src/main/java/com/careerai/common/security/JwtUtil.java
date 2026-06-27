package com.careerai.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Stateless helper for issuing and validating HS256 JWTs (JJWT 0.12.x API).
 * Shared by every service so token claims stay consistent across the platform.
 */
public class JwtUtil {

    private final SecretKey key;

    public JwtUtil(String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String subject, Map<String, Object> claims, long ttlMillis) {
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(parseClaims(token));
    }

    public boolean isValid(String token) {
        try {
            return parseClaims(token).getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
