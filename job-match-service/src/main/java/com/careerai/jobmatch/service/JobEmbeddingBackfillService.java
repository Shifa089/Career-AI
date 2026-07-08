package com.careerai.jobmatch.service;

import com.careerai.jobmatch.domain.entity.JobListing;
import com.careerai.jobmatch.repository.JobListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Recovers active jobs whose embedding was never generated (e.g. the embedding model was briefly
 * unavailable when the job was posted). Without this, such a job would be invisible to the RAG
 * matcher forever; with it, the job becomes matchable on the next run. Latest-jobs browsing does not
 * depend on embeddings, so candidates can always see the job in the meantime.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JobEmbeddingBackfillService {

    private static final int BATCH_SIZE = 25;

    private final JobListingRepository jobListingRepository;
    private final JobEmbeddingIndexer jobEmbeddingIndexer;

    /** Runs a few minutes after startup and then every 15 minutes. */
    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT15M")
    public void backfillMissingEmbeddings() {
        List<JobListing> pending = jobListingRepository.findActiveWithoutEmbedding(PageRequest.of(0, BATCH_SIZE));
        if (pending.isEmpty()) {
            return;
        }
        int indexed = 0;
        for (JobListing listing : pending) {
            try {
                jobEmbeddingIndexer.index(listing);
                indexed++;
            } catch (Exception e) {
                log.warn("Backfill embedding failed for job {} (will retry next run): {}",
                        listing.getId(), e.getMessage());
            }
        }
        log.info("Embedding backfill indexed {}/{} pending job(s)", indexed, pending.size());
    }
}
