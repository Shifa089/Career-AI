package com.careerai.jobmatch.repository;

import com.careerai.jobmatch.domain.entity.JobMatch;
import com.careerai.jobmatch.domain.enums.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobMatchRepository extends JpaRepository<JobMatch, UUID> {

    Page<JobMatch> findByUserId(UUID userId, Pageable pageable);

    Page<JobMatch> findByUserIdAndStatus(UUID userId, MatchStatus status, Pageable pageable);

    Optional<JobMatch> findByIdAndUserId(UUID id, UUID userId);

    Optional<JobMatch> findByUserIdAndResumeIdAndJobListingId(UUID userId, UUID resumeId, UUID jobListingId);

    /**
     * pgvector cosine-similarity search: returns {@code [job_listing_id (UUID), similarity (double)]}
     * rows for active listings that have an embedding, best match first. {@code similarity = 1 -
     * cosine_distance}, so higher is better. The embedding is bound as the pgvector text literal
     * {@code [a,b,c]} and cast to {@code vector} in SQL.
     */
    @Query(value = """
            SELECT je.job_listing_id, 1 - (je.embedding <=> CAST(:embedding AS vector)) AS similarity_score
            FROM job_listings jl
            JOIN job_embeddings je ON jl.id = je.job_listing_id
            WHERE jl.active = true AND je.embedding IS NOT NULL
            ORDER BY je.embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findSimilarJobs(@Param("embedding") String embedding, @Param("limit") int limit);
}
