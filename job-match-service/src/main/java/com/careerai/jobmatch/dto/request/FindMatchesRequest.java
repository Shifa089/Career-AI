package com.careerai.jobmatch.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request to compute semantic job matches for a given resume.
 *
 * @param resumeId   the resume whose embedding drives the match (required)
 * @param limit      max number of matches to return (1-50); defaults to 10 when null
 * @param targetRole optional role hint to bias the search (currently advisory)
 */
public record FindMatchesRequest(
        @NotNull UUID resumeId,
        @Min(1) @Max(50) Integer limit,
        String targetRole) {

    public int limitOrDefault() {
        return limit == null ? 10 : limit;
    }
}
