package com.careerai.jobmatch.service;

import com.careerai.common.exception.ResourceNotFoundException;
import com.careerai.jobmatch.domain.entity.JobEmbedding;
import com.careerai.jobmatch.domain.entity.JobListing;
import com.careerai.jobmatch.domain.enums.JobSource;
import com.careerai.jobmatch.dto.request.CreateJobRequest;
import com.careerai.jobmatch.dto.response.JobListingResponse;
import com.careerai.jobmatch.mapper.JobMatchMapper;
import com.careerai.jobmatch.repository.JobEmbeddingRepository;
import com.careerai.jobmatch.repository.JobListingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Persists employer-posted jobs and indexes them for semantic matching by generating an embedding
 * (mirrors the seed/Adzuna ingestion path). Skill lists are stored as JSON-array TEXT to match the
 * existing {@link JobListing} column contract.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmployerJobServiceImpl implements EmployerJobService {

    private final JobListingRepository jobListingRepository;
    private final JobEmbeddingRepository jobEmbeddingRepository;
    private final EmbeddingService embeddingService;
    private final JobMatchMapper jobMatchMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public JobListingResponse postJob(CreateJobRequest request, UUID employerId) {
        JobListing listing = JobListing.builder()
                .title(request.title())
                .company(request.company())
                .location(request.location())
                .jobType(request.jobType())
                .descriptionText(request.descriptionText())
                .requiredSkills(toJson(request.requiredSkills()))
                .niceToHaveSkills(toJson(request.niceToHaveSkills()))
                .salaryRange(request.salaryRange())
                .experienceLevel(request.experienceLevel())
                .employerId(employerId)
                .source(JobSource.EMPLOYER)
                .active(true)
                .postedAt(LocalDateTime.now())
                .build();
        JobListing saved = jobListingRepository.save(listing);

        indexForMatching(saved);

        return jobMatchMapper.toJobListingResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobListingResponse> getMyJobs(UUID employerId, Pageable pageable) {
        return jobListingRepository.findByEmployerIdOrderByCreatedAtDesc(employerId, pageable)
                .map(jobMatchMapper::toJobListingResponse);
    }

    @Override
    @Transactional
    public JobListingResponse setJobActive(UUID jobId, UUID employerId, boolean active) {
        JobListing listing = jobListingRepository.findByIdAndEmployerId(jobId, employerId)
                .orElseThrow(() -> new ResourceNotFoundException("Job listing", jobId));
        listing.setActive(active);
        return jobMatchMapper.toJobListingResponse(jobListingRepository.save(listing));
    }

    /**
     * Best-effort embedding so a posted job is immediately matchable. A missing/failed embedding
     * model must not fail the post — the job is still persisted and can be re-embedded later.
     */
    private void indexForMatching(JobListing listing) {
        try {
            String text = listing.getTitle() + ". " + listing.getDescriptionText();
            float[] embedding = embeddingService.generateEmbedding(text);
            jobEmbeddingRepository.save(JobEmbedding.builder()
                    .jobListing(listing)
                    .embedding(embedding)
                    .build());
        } catch (Exception e) {
            log.warn("Could not embed posted job {} (is the embedding model configured?): {}",
                    listing.getId(), e.getMessage());
        }
    }

    private String toJson(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
