package com.careerai.jobmatch.dto.request;

import com.careerai.jobmatch.domain.enums.ExperienceLevel;
import com.careerai.jobmatch.domain.enums.JobType;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Payload for an employer posting a new job to the portal.
 */
public record CreateJobRequest(

        @NotBlank(message = "Job title is required")
        String title,

        @NotBlank(message = "Company name is required")
        String company,

        String location,

        JobType jobType,

        @NotBlank(message = "Job description is required")
        String descriptionText,

        List<String> requiredSkills,

        List<String> niceToHaveSkills,

        String salaryRange,

        ExperienceLevel experienceLevel
) {
}
