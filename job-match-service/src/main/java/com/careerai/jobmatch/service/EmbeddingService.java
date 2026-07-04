package com.careerai.jobmatch.service;

import com.careerai.jobmatch.domain.entity.ResumeEmbedding;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Generates and persists semantic embeddings used by the RAG matching pipeline.
 */
public interface EmbeddingService {

    /**
     * Embeds arbitrary text into a 1536-dimension vector. Long text is truncated to a sensible
     * window before embedding.
     */
    float[] generateEmbedding(String text);

    /**
     * Generates (or refreshes) the embedding for a resume and persists it, keyed by resume id.
     */
    ResumeEmbedding generateAndSaveResumeEmbedding(UUID resumeId, UUID userId, String resumeText, List<String> skills);

    /**
     * Retrieves a stored resume embedding (cached).
     */
    Optional<ResumeEmbedding> getResumeEmbedding(UUID resumeId);
}
