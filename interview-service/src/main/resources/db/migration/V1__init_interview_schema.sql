-- Interview service baseline schema. UUID primary keys per CareerAI convention.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE interview_sessions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    role        VARCHAR(255) NOT NULL,
    difficulty  VARCHAR(50)  NOT NULL DEFAULT 'MEDIUM',
    status      VARCHAR(50)  NOT NULL DEFAULT 'IN_PROGRESS',
    started_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ended_at    TIMESTAMPTZ,
    overall_score NUMERIC(5, 2)
);

CREATE TABLE interview_messages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id  UUID NOT NULL REFERENCES interview_sessions (id) ON DELETE CASCADE,
    sender      VARCHAR(20)  NOT NULL,
    content     TEXT         NOT NULL,
    sequence_no INTEGER      NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_interview_sessions_user ON interview_sessions (user_id);
CREATE INDEX idx_interview_messages_session ON interview_messages (session_id);
