-- Job-match service baseline schema. UUID primary keys + pgvector embeddings.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE jobs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title        VARCHAR(255) NOT NULL,
    company      VARCHAR(255) NOT NULL,
    location     VARCHAR(255),
    description  TEXT         NOT NULL,
    seniority    VARCHAR(50),
    source       VARCHAR(100),
    external_id  VARCHAR(255),
    embedding    vector(1536),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE job_matches (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL,
    job_id        UUID NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    similarity    NUMERIC(6, 5) NOT NULL,
    rationale     TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_jobs_embedding ON jobs USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_job_matches_user ON job_matches (user_id);
