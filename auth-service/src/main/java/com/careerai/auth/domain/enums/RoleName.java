package com.careerai.auth.domain.enums;

/**
 * Application roles. Each value is used verbatim as a Spring Security authority,
 * so the {@code ROLE_} prefix is intentional.
 */
public enum RoleName {
    ROLE_USER,
    ROLE_ADMIN,
    ROLE_PREMIUM
}
