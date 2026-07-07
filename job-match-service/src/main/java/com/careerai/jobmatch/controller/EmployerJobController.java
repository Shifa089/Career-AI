package com.careerai.jobmatch.controller;

import com.careerai.common.dto.ApiResponse;
import com.careerai.jobmatch.dto.request.CreateJobRequest;
import com.careerai.jobmatch.dto.response.JobListingResponse;
import com.careerai.jobmatch.security.AuthenticatedUser;
import com.careerai.jobmatch.service.EmployerJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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

import java.util.UUID;

/**
 * Employer-only job management. Guarded by {@code ROLE_COMPANY}; the caller's identity (the owning
 * employer) is taken from the gateway-forwarded {@code X-User-*} headers. Posted jobs are embedded
 * immediately so they surface in candidate matching.
 */
@RestController
@RequestMapping("/api/v1/job-matches/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY')")
@Tag(name = "Employer Jobs", description = "Post and manage jobs as a company / employer")
public class EmployerJobController {

    private final EmployerJobService employerJobService;

    @Operation(summary = "Post a new job to the portal")
    @PostMapping
    public ResponseEntity<ApiResponse<JobListingResponse>> postJob(@Valid @RequestBody CreateJobRequest request) {
        AuthenticatedUser user = AuthenticatedUser.current();
        JobListingResponse created = employerJobService.postJob(request, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Job posted", created));
    }

    @Operation(summary = "List the current employer's posted jobs")
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<Page<JobListingResponse>>> getMyJobs(Pageable pageable) {
        AuthenticatedUser user = AuthenticatedUser.current();
        return ResponseEntity.ok(ApiResponse.success(employerJobService.getMyJobs(user.userId(), pageable)));
    }

    @Operation(summary = "Open or close one of the current employer's jobs")
    @PatchMapping("/{jobId}/active")
    public ResponseEntity<ApiResponse<JobListingResponse>> setActive(
            @PathVariable UUID jobId,
            @RequestParam("active") boolean active) {
        AuthenticatedUser user = AuthenticatedUser.current();
        return ResponseEntity.ok(ApiResponse.success("Job updated",
                employerJobService.setJobActive(jobId, user.userId(), active)));
    }
}
