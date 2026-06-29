package com.careerai.interview.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Trusts the identity headers injected by the API gateway (which performs the actual JWT
 * validation) and populates the security context. This service never re-validates tokens.
 */
@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_EMAIL_HEADER = "X-User-Email";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader(USER_ID_HEADER);
        if (StringUtils.hasText(userId) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                AuthenticatedUser principal =
                        new AuthenticatedUser(UUID.fromString(userId.trim()), request.getHeader(USER_EMAIL_HEADER));

                List<SimpleGrantedAuthority> authorities = parseRoles(request.getHeader(USER_ROLES_HEADER));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (IllegalArgumentException ex) {
                // Malformed X-User-Id: leave the context unauthenticated; the entry point handles it.
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> parseRoles(String rolesHeader) {
        if (!StringUtils.hasText(rolesHeader)) {
            return List.of();
        }
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
