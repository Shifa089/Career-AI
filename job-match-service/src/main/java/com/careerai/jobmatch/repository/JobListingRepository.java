package com.careerai.jobmatch.repository;

import com.careerai.jobmatch.domain.entity.JobListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JobListingRepository extends JpaRepository<JobListing, UUID> {

    boolean existsByExternalId(String externalId);

    List<JobListing> findByActiveTrue();

    Page<JobListing> findByEmployerIdOrderByCreatedAtDesc(UUID employerId, Pageable pageable);

    java.util.Optional<JobListing> findByIdAndEmployerId(UUID id, UUID employerId);

    /**
     * The candidate-facing "latest jobs" feed: all active listings, newest first. Ordered by
     * {@code postedAt} (nulls last for seed/ingested rows that predate the field) then {@code createdAt}
     * so a freshly posted job always surfaces at the top.
     */
    @Query("""
            SELECT j FROM JobListing j
            WHERE j.active = true
            ORDER BY j.postedAt DESC NULLS LAST, j.createdAt DESC
            """)
    Page<JobListing> findActiveLatest(Pageable pageable);

    /**
     * Free-text search over active listings by keyword (title/company/description) and location. A
     * {@code null} filter is treated as "no constraint". Results are newest-first.
     */
    @Query("""
            SELECT j FROM JobListing j
            WHERE j.active = true
              AND (:keyword IS NULL
                   OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(j.company) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(j.descriptionText) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%')))
            ORDER BY j.postedAt DESC NULLS LAST, j.createdAt DESC
            """)
    Page<JobListing> search(@Param("keyword") String keyword,
                            @Param("location") String location,
                            Pageable pageable);

    /**
     * Active listings that have no embedding yet — used by the embedding-backfill job to recover jobs
     * whose embedding generation failed at post time (so they still become matchable).
     */
    @Query("""
            SELECT j FROM JobListing j
            WHERE j.active = true
              AND NOT EXISTS (SELECT 1 FROM JobEmbedding e WHERE e.jobListing = j)
            """)
    List<JobListing> findActiveWithoutEmbedding(Pageable pageable);
}
