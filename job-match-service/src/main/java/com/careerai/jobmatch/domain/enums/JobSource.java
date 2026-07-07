package com.careerai.jobmatch.domain.enums;

/**
 * Where a {@link com.careerai.jobmatch.domain.entity.JobListing} originated.
 */
public enum JobSource {
    /** Seeded demo data. */
    SEED,
    /** Ingested from the Adzuna feed. */
    ADZUNA,
    /** Posted directly by an employer (company HR) through the portal. */
    EMPLOYER
}
