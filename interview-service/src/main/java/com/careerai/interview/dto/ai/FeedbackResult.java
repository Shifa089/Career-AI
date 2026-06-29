package com.careerai.interview.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Deserialization target for Claude's JSON final-feedback response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FeedbackResult(
        Integer technicalScore,
        Integer behaviouralScore,
        Integer communicationScore,
        Integer problemSolvingScore,
        Integer overallScore,
        List<String> strongAreas,
        List<String> improvementAreas,
        String detailedFeedback,
        List<RecommendedResource> recommendedResources
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecommendedResource(String title, String url, String type) {
    }
}
