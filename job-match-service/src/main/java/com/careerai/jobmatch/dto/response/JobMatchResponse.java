package com.careerai.jobmatch.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A scored match between a user's resume and a job listing.
 */
public record JobMatchResponse(
        UUID matchId,
        JobListingResponse job,
        Double similarityScore,
        Integer matchPercentage,
        List<String> matchedSkills,
        List<String> missingSkills,
        String status,
        LocalDateTime createdAt) {
}
