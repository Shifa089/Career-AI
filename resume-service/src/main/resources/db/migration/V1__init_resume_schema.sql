-- Resume service baseline schema. UUID primary keys per CareerAI convention.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE resumes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    s3_key      VARCHAR(512) NOT NULL,
    content_type VARCHAR(100),
    size_bytes  BIGINT,
    raw_text    TEXT,
    status      VARCHAR(50)  NOT NULL DEFAULT 'UPLOADED',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE extracted_skills (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id   UUID NOT NULL REFERENCES resumes (id) ON DELETE CASCADE,
    skill       VARCHAR(255) NOT NULL,
    category    VARCHAR(100),
    confidence  NUMERIC(4, 3),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_resumes_user ON resumes (user_id);
CREATE INDEX idx_extracted_skills_resume ON extracted_skills (resume_id);
