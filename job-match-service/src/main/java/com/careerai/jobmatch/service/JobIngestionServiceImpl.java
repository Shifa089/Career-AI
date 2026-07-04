package com.careerai.jobmatch.service;

import com.careerai.jobmatch.domain.entity.JobEmbedding;
import com.careerai.jobmatch.domain.entity.JobListing;
import com.careerai.jobmatch.domain.enums.JobType;
import com.careerai.jobmatch.repository.JobEmbeddingRepository;
import com.careerai.jobmatch.repository.JobListingRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.List;

/**
 * {@link JobIngestionService} that pulls jobs from Adzuna (when configured) and backfills embeddings
 * for seeded listings. On startup it embeds any sample jobs lacking an embedding; a daily schedule
 * refreshes from Adzuna.
 */
@Service
@Slf4j
public class JobIngestionServiceImpl implements JobIngestionService {

    private final WebClient adzunaWebClient;
    private final JobListingRepository jobListingRepository;
    private final JobEmbeddingRepository jobEmbeddingRepository;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    private final String country;
    private final String appId;
    private final String appKey;
    private final boolean seedSampleJobs;

    public JobIngestionServiceImpl(WebClient adzunaWebClient,
                                   JobListingRepository jobListingRepository,
                                   JobEmbeddingRepository jobEmbeddingRepository,
                                   EmbeddingService embeddingService,
                                   ObjectMapper objectMapper,
                                   @Value("${adzuna.country:gb}") String country,
                                   @Value("${adzuna.app-id:}") String appId,
                                   @Value("${adzuna.app-key:}") String appKey,
                                   @Value("${job-match.ingestion.seed-sample-jobs:true}") boolean seedSampleJobs) {
        this.adzunaWebClient = adzunaWebClient;
        this.jobListingRepository = jobListingRepository;
        this.jobEmbeddingRepository = jobEmbeddingRepository;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
        this.country = country;
        this.appId = appId;
        this.appKey = appKey;
        this.seedSampleJobs = seedSampleJobs;
    }

    /**
     * On startup, embed seeded sample jobs so they are immediately matchable. Failures (e.g. no
     * embedding API key in local dev) are logged but never block application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    void onStartup() {
        if (!seedSampleJobs) {
            return;
        }
        try {
            int embedded = ingestSampleJobs();
            if (embedded > 0) {
                log.info("Backfilled embeddings for {} seeded job listing(s)", embedded);
            }
        } catch (Exception e) {
            log.warn("Could not backfill sample-job embeddings on startup (is the embedding model configured?): {}",
                    e.getMessage());
        }
    }

    @Override
    public int ingestSampleJobs() {
        int embedded = 0;
        for (JobListing listing : jobListingRepository.findByActiveTrue()) {
            if (jobEmbeddingRepository.existsByJobListingId(listing.getId())) {
                continue;
            }
            embedAndSave(listing);
            embedded++;
        }
        return embedded;
    }

    @Override
    public int ingestFromAdzuna(String keywords, String location, int count) {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appKey)) {
            log.warn("Adzuna credentials not configured; skipping live ingestion");
            return 0;
        }

        AdzunaResponse response;
        try {
            response = adzunaWebClient.get()
                    .uri(uriBuilder -> UriComponentsBuilder.fromUri(uriBuilder.build())
                            .path("/jobs/{country}/search/1")
                            .queryParam("app_id", appId)
                            .queryParam("app_key", appKey)
                            .queryParam("results_per_page", count)
                            .queryParam("what", keywords == null ? "" : keywords)
                            .queryParam("where", location == null ? "" : location)
                            .queryParam("content-type", "application/json")
                            .build(country))
                    .retrieve()
                    .bodyToMono(AdzunaResponse.class)
                    .block(Duration.ofSeconds(30));
        } catch (Exception e) {
            log.error("Adzuna ingestion failed for '{}' / '{}': {}", keywords, location, e.getMessage());
            return 0;
        }

        if (response == null || response.results() == null) {
            return 0;
        }

        int ingested = 0;
        for (AdzunaJob job : response.results()) {
            String externalId = "adzuna-" + job.id();
            if (jobListingRepository.existsByExternalId(externalId)) {
                continue;
            }
            saveAdzunaJob(job, externalId);
            ingested++;
        }
        log.info("Ingested {} new job(s) from Adzuna for '{}'", ingested, keywords);
        return ingested;
    }

    /**
     * Daily refresh from Adzuna for a few common software roles (no-op when Adzuna is unconfigured).
     */
    @Scheduled(cron = "0 0 6 * * *")
    void scheduleIngestion() {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appKey)) {
            return;
        }
        ingestFromAdzuna("software engineer", "", 25);
        ingestFromAdzuna("data engineer", "", 25);
    }

    private void saveAdzunaJob(AdzunaJob job, String externalId) {
        JobListing listing = JobListing.builder()
                .title(job.title())
                .company(job.company() != null ? job.company().displayName() : "Unknown")
                .location(job.location() != null ? job.location().displayName() : null)
                .descriptionText(job.description() != null ? job.description() : "")
                .salaryRange(formatSalary(job.salaryMin(), job.salaryMax()))
                .jobType(inferJobType(job))
                .sourceUrl(job.redirectUrl())
                .externalId(externalId)
                .active(true)
                .build();
        JobListing saved = jobListingRepository.save(listing);
        embedAndSave(saved);
    }

    private void embedAndSave(JobListing listing) {
        String text = listing.getTitle() + ". " + listing.getDescriptionText();
        float[] embedding = embeddingService.generateEmbedding(text);
        jobEmbeddingRepository.save(JobEmbedding.builder()
                .jobListing(listing)
                .embedding(embedding)
                .build());
    }

    private JobType inferJobType(AdzunaJob job) {
        String haystack = ((job.title() == null ? "" : job.title()) + " "
                + (job.description() == null ? "" : job.description())).toLowerCase();
        if (haystack.contains("remote")) {
            return JobType.REMOTE;
        }
        if (haystack.contains("hybrid")) {
            return JobType.HYBRID;
        }
        return JobType.ONSITE;
    }

    private String formatSalary(Double min, Double max) {
        if (min == null && max == null) {
            return null;
        }
        if (min != null && max != null) {
            return String.format("%,.0f-%,.0f", min, max);
        }
        return String.format("%,.0f", min != null ? min : max);
    }

    // --- Adzuna response shapes (only the fields we use) ----------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AdzunaResponse(List<AdzunaJob> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AdzunaJob(
            String id,
            String title,
            String description,
            AdzunaCompany company,
            AdzunaLocation location,
            @JsonProperty("redirect_url") String redirectUrl,
            @JsonProperty("salary_min") Double salaryMin,
            @JsonProperty("salary_max") Double salaryMax) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AdzunaCompany(@JsonProperty("display_name") String displayName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AdzunaLocation(@JsonProperty("display_name") String displayName) {
    }
}
