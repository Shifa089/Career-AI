package com.careerai.jobmatch.service;

import com.careerai.jobmatch.domain.entity.JobListing;
import com.careerai.jobmatch.domain.enums.MatchStatus;
import com.careerai.jobmatch.dto.response.JobListingResponse;
import com.careerai.jobmatch.dto.response.JobMatchResponse;
import com.careerai.jobmatch.dto.response.LearningPathResponse;
import com.careerai.jobmatch.dto.response.SkillGapResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Core RAG job-matching operations: semantic match computation, match lifecycle, job search, and
 * Claude-backed skill-gap / learning-path analysis for a match.
 */
public interface JobMatchService {

    /**
     * Computes (and caches) the top {@code limit} semantic matches for a resume, persisting a
     * {@link com.careerai.jobmatch.domain.entity.JobMatch} per result.
     */
    List<JobMatchResponse> findMatchesForResume(UUID resumeId, UUID userId, int limit);

    /**
     * Persists a job listing and generates + stores its embedding.
     */
    JobListing saveJob(JobListing listing);

    JobMatchResponse getMatchById(UUID matchId, UUID userId);

    Page<JobMatchResponse> getMyMatches(UUID userId, MatchStatus status, Pageable pageable);

    JobMatchResponse updateMatchStatus(UUID matchId, MatchStatus status, UUID userId);

    Page<JobListingResponse> searchJobs(String keyword, String location, Pageable pageable);

    SkillGapResponse analyseSkillGap(UUID matchId, UUID userId);

    LearningPathResponse getLearningPath(UUID matchId, UUID userId);
}
