package com.careerai.jobmatch.service;

/**
 * Ingests job listings into the catalog from external sources, and backfills embeddings for seeded
 * jobs so they become matchable.
 */
public interface JobIngestionService {

    /**
     * Fetches jobs from the Adzuna API and persists any not already present (by external id).
     *
     * @return the number of new listings ingested
     */
    int ingestFromAdzuna(String keywords, String location, int count);

    /**
     * Generates and stores embeddings for any active listing that does not yet have one (e.g. the
     * Flyway-seeded sample jobs). Idempotent.
     *
     * @return the number of listings embedded
     */
    int ingestSampleJobs();
}
