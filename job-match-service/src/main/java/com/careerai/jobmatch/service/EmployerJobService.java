package com.careerai.jobmatch.service;

import com.careerai.jobmatch.dto.request.CreateJobRequest;
import com.careerai.jobmatch.dto.response.JobListingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Employer-facing job management: posting jobs to the portal (which are immediately embedded and
 * become matchable by candidates), listing an employer's own jobs, and opening/closing them.
 */
public interface EmployerJobService {

    JobListingResponse postJob(CreateJobRequest request, UUID employerId);

    Page<JobListingResponse> getMyJobs(UUID employerId, Pageable pageable);

    JobListingResponse setJobActive(UUID jobId, UUID employerId, boolean active);
}
