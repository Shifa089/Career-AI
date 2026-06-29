package com.careerai.jobmatch.controller;

import com.careerai.common.dto.ApiResponse;
import com.careerai.jobmatch.domain.enums.MatchStatus;
import com.careerai.jobmatch.dto.request.FindMatchesRequest;
import com.careerai.jobmatch.dto.response.JobListingResponse;
import com.careerai.jobmatch.dto.response.JobMatchResponse;
import com.careerai.jobmatch.dto.response.LearningPathResponse;
import com.careerai.jobmatch.dto.response.SkillGapResponse;
import com.careerai.jobmatch.security.AuthenticatedUser;
import com.careerai.jobmatch.service.JobIngestionService;
import com.careerai.jobmatch.service.JobMatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Job-matching endpoints. The caller's identity is taken from the gateway-forwarded {@code X-User-*}
 * headers (see {@code HeaderAuthenticationFilter}); all responses are wrapped in {@link ApiResponse}.
 */
@RestController
@RequestMapping("/api/v1/job-matches")
@RequiredArgsConstructor
@Tag(name = "Job Matches", description = "Semantic job matching, skill-gap analysis and learning paths")
public class JobMatchController {

    private final JobMatchService jobMatchService;
    private final JobIngestionService jobIngestionService;

    @Operation(summary = "Compute semantic job matches for a resume")
    @PostMapping("/find")
    public ResponseEntity<ApiResponse<List<JobMatchResponse>>> findMatches(
            @Valid @RequestBody FindMatchesRequest request) {
        AuthenticatedUser user = AuthenticatedUser.current();
        List<JobMatchResponse> matches =
                jobMatchService.findMatchesForResume(request.resumeId(), user.userId(), request.limitOrDefault());
        return ResponseEntity.ok(ApiResponse.success("Matches computed", matches));
    }

    @Operation(summary = "List the current user's matches, optionally filtered by status")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<JobMatchResponse>>> getMyMatches(
            @RequestParam(value = "status", required = false) MatchStatus status,
            Pageable pageable) {
        AuthenticatedUser user = AuthenticatedUser.current();
        return ResponseEntity.ok(ApiResponse.success(jobMatchService.getMyMatches(user.userId(), status, pageable)));
    }

    @Operation(summary = "Get a single match")
    @GetMapping("/{matchId}")
    public ResponseEntity<ApiResponse<JobMatchResponse>> getMatch(@PathVariable UUID matchId) {
        AuthenticatedUser user = AuthenticatedUser.current();
        return ResponseEntity.ok(ApiResponse.success(jobMatchService.getMatchById(matchId, user.userId())));
    }

    @Operation(summary = "Claude-powered skill-gap analysis for a match")
    @GetMapping("/{matchId}/skill-gap")
    public ResponseEntity<ApiResponse<SkillGapResponse>> getSkillGap(@PathVariable UUID matchId) {
        AuthenticatedUser user = AuthenticatedUser.current();
        return ResponseEntity.ok(ApiResponse.success(jobMatchService.analyseSkillGap(matchId, user.userId())));
    }

    @Operation(summary = "Prioritised learning path to close the skill gap for a match")
    @GetMapping("/{matchId}/learning-path")
    public ResponseEntity<ApiResponse<LearningPathResponse>> getLearningPath(@PathVariable UUID matchId) {
        AuthenticatedUser user = AuthenticatedUser.current();
        return ResponseEntity.ok(ApiResponse.success(jobMatchService.getLearningPath(matchId, user.userId())));
    }

    @Operation(summary = "Update the status of a match")
    @PatchMapping("/{matchId}/status")
    public ResponseEntity<ApiResponse<JobMatchResponse>> updateStatus(
            @PathVariable UUID matchId,
            @RequestParam("status") MatchStatus status) {
        AuthenticatedUser user = AuthenticatedUser.current();
        return ResponseEntity.ok(ApiResponse.success("Match status updated",
                jobMatchService.updateMatchStatus(matchId, status, user.userId())));
    }

    @Operation(summary = "Search the job catalog by keyword and location")
    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<Page<JobListingResponse>>> searchJobs(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "location", required = false) String location,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(jobMatchService.searchJobs(keyword, location, pageable)));
    }

    @Operation(summary = "Trigger Adzuna job ingestion (admin only)")
    @PostMapping("/jobs/ingest")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> ingest(
            @RequestParam(value = "keywords", defaultValue = "software engineer") String keywords,
            @RequestParam(value = "location", required = false, defaultValue = "") String location,
            @RequestParam(value = "count", defaultValue = "25") int count) {
        int ingested = jobIngestionService.ingestFromAdzuna(keywords, location, count);
        return ResponseEntity.ok(ApiResponse.success("Ingestion complete", Map.of("ingested", ingested)));
    }
}
