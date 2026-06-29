-- Interview service baseline schema. UUID primary keys per CareerAI convention.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE interview_sessions (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID         NOT NULL,
    user_email         VARCHAR(255) NOT NULL,
    job_title          VARCHAR(255) NOT NULL,
    job_description    TEXT,
    target_company     VARCHAR(255),
    type               VARCHAR(20)  NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'CREATED',
    total_questions    INTEGER      NOT NULL DEFAULT 10,
    questions_answered INTEGER      NOT NULL DEFAULT 0,
    overall_score      INTEGER,
    feedback_summary   TEXT,
    started_at         TIMESTAMP,
    completed_at       TIMESTAMP,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_interview_sessions_user_id ON interview_sessions (user_id);

CREATE TABLE interview_questions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID        NOT NULL REFERENCES interview_sessions (id) ON DELETE CASCADE,
    question_number INTEGER     NOT NULL,
    question_text   TEXT        NOT NULL,
    type            VARCHAR(20),
    difficulty      VARCHAR(10),
    user_answer     TEXT,
    answer_score    INTEGER,
    answer_feedback TEXT,
    ideal_answer    TEXT,
    skills_tested   TEXT,
    asked_at        TIMESTAMP,
    answered_at     TIMESTAMP
);

CREATE INDEX idx_interview_questions_session_id ON interview_questions (session_id);

CREATE TABLE interview_feedback (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id            UUID NOT NULL UNIQUE REFERENCES interview_sessions (id) ON DELETE CASCADE,
    technical_score       INTEGER,
    behavioural_score     INTEGER,
    communication_score   INTEGER,
    problem_solving_score INTEGER,
    strong_areas          TEXT,
    improvement_areas     TEXT,
    detailed_feedback     TEXT,
    recommended_resources TEXT,
    generated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_interview_feedback_session_id ON interview_feedback (session_id);
