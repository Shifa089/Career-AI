-- Job-match service baseline schema.
-- UUID primary keys (pgcrypto) + pgvector embedding columns for RAG semantic matching.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS vector;

-- ---------------------------------------------------------------------------
-- Job listings ingested from external sources (Adzuna) or seeded for demo.
-- ---------------------------------------------------------------------------
CREATE TABLE job_listings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title               VARCHAR(255) NOT NULL,
    company             VARCHAR(255) NOT NULL,
    location            VARCHAR(255),
    job_type            VARCHAR(20),                       -- REMOTE | HYBRID | ONSITE
    description_text    TEXT         NOT NULL,
    required_skills     TEXT,                              -- JSON array
    nice_to_have_skills TEXT,                              -- JSON array
    salary_range        VARCHAR(100),
    experience_level    VARCHAR(20),                       -- JUNIOR | MID | SENIOR | LEAD | PRINCIPAL
    source_url          VARCHAR(1024),
    external_id         VARCHAR(255),
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    posted_at           TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL,
    updated_at          TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX idx_job_listings_external_id ON job_listings (external_id) WHERE external_id IS NOT NULL;
CREATE INDEX idx_job_listings_active ON job_listings (active);

-- ---------------------------------------------------------------------------
-- One embedding per job listing (kept in its own table so the listing row stays
-- lightweight and embeddings can be regenerated independently).
-- ---------------------------------------------------------------------------
CREATE TABLE job_embeddings (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_listing_id UUID NOT NULL UNIQUE REFERENCES job_listings (id) ON DELETE CASCADE,
    embedding      vector(1536),
    created_at     TIMESTAMP NOT NULL
);

CREATE INDEX idx_job_embeddings_vector ON job_embeddings USING hnsw (embedding vector_cosine_ops);

-- ---------------------------------------------------------------------------
-- One embedding per analysed resume (built from resume text + extracted skills).
-- ---------------------------------------------------------------------------
CREATE TABLE resume_embeddings (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id        UUID NOT NULL,
    user_id          UUID NOT NULL,
    embedding        vector(1536),
    resume_text      TEXT,
    extracted_skills TEXT,                                 -- JSON array
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_resume_embeddings_resume_id ON resume_embeddings (resume_id);
CREATE INDEX idx_resume_embeddings_user_id ON resume_embeddings (user_id);
CREATE INDEX idx_resume_embeddings_vector ON resume_embeddings USING hnsw (embedding vector_cosine_ops);

-- ---------------------------------------------------------------------------
-- Materialised matches between a user's resume and a job listing.
-- ---------------------------------------------------------------------------
CREATE TABLE job_matches (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL,
    resume_id        UUID NOT NULL,
    job_listing_id   UUID NOT NULL REFERENCES job_listings (id) ON DELETE CASCADE,
    similarity_score DOUBLE PRECISION NOT NULL,
    match_percentage INTEGER,
    matched_skills   TEXT,                                 -- JSON array
    missing_skills   TEXT,                                 -- JSON array
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW',
    created_at       TIMESTAMP   NOT NULL
);

CREATE INDEX idx_job_matches_user_id ON job_matches (user_id);
CREATE INDEX idx_job_matches_resume_id ON job_matches (resume_id);
CREATE UNIQUE INDEX idx_job_matches_user_resume_job ON job_matches (user_id, resume_id, job_listing_id);
