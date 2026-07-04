package com.careerai.jobmatch.service;

import com.careerai.jobmatch.domain.entity.ResumeEmbedding;
import com.careerai.jobmatch.exception.EmbeddingGenerationException;
import com.careerai.jobmatch.repository.ResumeEmbeddingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link EmbeddingService} backed by Spring AI's {@link EmbeddingModel} (OpenAI
 * text-embedding-3-small, 1536-dim). Resume embeddings are persisted so matches can be recomputed
 * without re-embedding, and retrieval is cached in Redis.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SpringAiEmbeddingServiceImpl implements EmbeddingService {

    /** OpenAI embedding inputs are bounded; we summarise resumes to their first ~8k chars. */
    private static final int MAX_CHARS = 8000;

    private final EmbeddingModel embeddingModel;
    private final ResumeEmbeddingRepository resumeEmbeddingRepository;
    private final ObjectMapper objectMapper;

    @Override
    public float[] generateEmbedding(String text) {
        if (!StringUtils.hasText(text)) {
            throw new EmbeddingGenerationException("Cannot embed empty text");
        }
        String truncated = text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text;
        try {
            return embeddingModel.embed(truncated);
        } catch (Exception e) {
            throw new EmbeddingGenerationException("Embedding model call failed", e);
        }
    }

    @Override
    @Transactional
    public ResumeEmbedding generateAndSaveResumeEmbedding(UUID resumeId, UUID userId, String resumeText,
                                                          List<String> skills) {
        String skillsText = skills == null || skills.isEmpty() ? "" : " Skills: " + String.join(", ", skills);
        String combined = (resumeText == null ? "" : resumeText) + skillsText;
        float[] embedding = generateEmbedding(combined);

        ResumeEmbedding entity = resumeEmbeddingRepository.findByResumeId(resumeId)
                .orElseGet(() -> ResumeEmbedding.builder().resumeId(resumeId).build());
        entity.setUserId(userId);
        entity.setEmbedding(embedding);
        entity.setResumeText(resumeText);
        entity.setExtractedSkills(toJson(skills));

        ResumeEmbedding saved = resumeEmbeddingRepository.save(entity);
        log.info("Saved resume embedding for resume {} (user {})", resumeId, userId);
        return saved;
    }

    @Override
    @Cacheable(value = "embeddings", key = "#resumeId")
    @Transactional(readOnly = true)
    public Optional<ResumeEmbedding> getResumeEmbedding(UUID resumeId) {
        return resumeEmbeddingRepository.findByResumeId(resumeId);
    }

    private String toJson(List<String> values) {
        if (values == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
