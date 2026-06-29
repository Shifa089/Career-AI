package com.careerai.jobmatch.repository;

import com.careerai.jobmatch.domain.entity.ResumeEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResumeEmbeddingRepository extends JpaRepository<ResumeEmbedding, UUID> {

    Optional<ResumeEmbedding> findByResumeId(UUID resumeId);
}
