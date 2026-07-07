package com.careerai.jobmatch.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Public view of a {@link com.careerai.jobmatch.domain.entity.JobListing}. Skill JSON columns are
 * expanded into lists by the mapper.
 */
public record JobListingResponse(
        UUID id,
        String title,
        String company,
        String location,
        String jobType,
        String descriptionText,
        List<String> requiredSkills,
        List<String> niceToHaveSkills,
        String salaryRange,
        String experienceLevel,
        String sourceUrl,
        String source,
        UUID employerId,
        boolean active,
        LocalDateTime postedAt) {
}
