package com.careerai.jobmatch.dto.response;

import java.util.List;

/**
 * Claude-generated skill-gap analysis for a candidate against a specific job.
 */
public record SkillGapResponse(
        String jobTitle,
        List<String> matchedSkills,
        List<String> missingSkills,
        List<PartialMatch> partialMatches,
        Integer gapScore,
        String readinessLevel,
        String summary) {

    /**
     * A skill the candidate partially holds, with their level vs. the level the role expects.
     */
    public record PartialMatch(String skill, String candidateLevel, String requiredLevel) {
    }
}
