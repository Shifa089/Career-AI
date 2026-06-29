package com.careerai.jobmatch.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Deserialization target for Claude's skill-gap JSON response. Field names mirror the prompt's JSON
 * schema exactly so Jackson can bind directly.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillGapResult(
        List<String> matchedSkills,
        List<String> missingSkills,
        List<PartialMatch> partialMatches,
        Integer gapScore,
        String readinessLevel,
        List<LearningPathItem> learningPath,
        String summary) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PartialMatch(String skill, String candidateLevel, String requiredLevel) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LearningPathItem(
            String skill,
            String priority,
            Integer estimatedWeeks,
            List<LearningResource> resources) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LearningResource(String title, String url, String type) {
    }
}
