-- Auth service baseline schema. UUID primary keys per CareerAI convention.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    full_name     VARCHAR(255),
    provider      VARCHAR(50)  NOT NULL DEFAULT 'LOCAL',
    provider_id   VARCHAR(255),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE roles (
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token      VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN');

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
