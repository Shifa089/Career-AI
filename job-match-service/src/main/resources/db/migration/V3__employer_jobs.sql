-- Support jobs posted directly by employers (company HR users) alongside seeded/ingested jobs.

ALTER TABLE job_listings ADD COLUMN employer_id UUID;
ALTER TABLE job_listings ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'SEED';

-- Existing ingested rows carry an external_id from Adzuna.
UPDATE job_listings SET source = 'ADZUNA' WHERE external_id IS NOT NULL;

CREATE INDEX idx_job_listings_employer ON job_listings (employer_id);
