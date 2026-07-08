package com.careerai.jobmatch.service;

import com.careerai.jobmatch.domain.entity.JobEmbedding;
import com.careerai.jobmatch.domain.entity.JobListing;
import com.careerai.jobmatch.repository.JobEmbeddingRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Central place that turns a {@link JobListing} into its semantic embedding so a posted job becomes
 * matchable by the RAG pipeline. Used both at post time (best-effort) and by the backfill job. The
 * embedding text mirrors the seed/ingestion path ({@code JobMatchServiceImpl#saveJob}): title +
 * description + required skills, so employer-posted and ingested jobs are embedded consistently.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JobEmbeddingIndexer {

    private final JobEmbeddingRepository jobEmbeddingRepository;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    /**
     * Generates and persists the embedding for a listing, unless one already exists (idempotent).
     * Throws on failure so callers can decide how to handle it (post time swallows; backfill logs).
     */
    @Transactional
    public void index(JobListing listing) {
        if (jobEmbeddingRepository.existsByJobListingId(listing.getId())) {
            return;
        }
        float[] embedding = embeddingService.generateEmbedding(buildEmbeddingText(listing));
        jobEmbeddingRepository.save(JobEmbedding.builder()
                .jobListing(listing)
                .embedding(embedding)
                .build());
        log.info("Indexed job '{}' at {} for semantic matching", listing.getTitle(), listing.getCompany());
    }

    String buildEmbeddingText(JobListing listing) {
        StringBuilder text = new StringBuilder(listing.getTitle()).append(". ")
                .append(listing.getDescriptionText());
        List<String> skills = parseSkills(listing.getRequiredSkills());
        if (!skills.isEmpty()) {
            text.append(" Required skills: ").append(String.join(", ", skills));
        }
        return text.toString();
    }

    private List<String> parseSkills(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
            return values == null ? List.of() : values;
        } catch (Exception e) {
            return List.of();
        }
    }
}
