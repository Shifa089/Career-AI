CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE resumes (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID         NOT NULL,
    user_email         VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    s3_key             VARCHAR(512) NOT NULL,
    s3_url             VARCHAR(1024),
    content_type       VARCHAR(100) NOT NULL,
    file_size_bytes    BIGINT       NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'UPLOADED',
    extracted_text     TEXT,
    is_primary         BOOLEAN      NOT NULL DEFAULT FALSE,
    version            INTEGER      NOT NULL DEFAULT 1,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    analysed_at        TIMESTAMP
);

CREATE INDEX idx_resumes_user_id ON resumes (user_id);

CREATE TABLE resume_analyses (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id           UUID    NOT NULL UNIQUE REFERENCES resumes (id) ON DELETE CASCADE,
    ats_score           INTEGER,
    summary             TEXT,
    strengths           TEXT,
    weaknesses          TEXT,
    suggestions         TEXT,
    keywords            TEXT,
    missing_keywords    TEXT,
    target_roles        TEXT,
    years_of_experience INTEGER,
    education_level     VARCHAR(30),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE skill_extractions (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    analysis_id           UUID         NOT NULL REFERENCES resume_analyses (id) ON DELETE CASCADE,
    skill_name            VARCHAR(255) NOT NULL,
    category              VARCHAR(30),
    proficiency_level     VARCHAR(20),
    years_used            INTEGER,
    inferred_from_context BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_skill_extractions_analysis_id ON skill_extractions (analysis_id);
