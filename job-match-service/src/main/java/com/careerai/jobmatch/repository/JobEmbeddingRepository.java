package com.careerai.jobmatch.repository;

import com.careerai.jobmatch.domain.entity.JobEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobEmbeddingRepository extends JpaRepository<JobEmbedding, UUID> {

    boolean existsByJobListingId(UUID jobListingId);
}
