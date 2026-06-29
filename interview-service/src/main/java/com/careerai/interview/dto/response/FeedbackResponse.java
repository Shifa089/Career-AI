package com.careerai.interview.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Comprehensive final feedback for a completed interview session.
 */
public record FeedbackResponse(
        UUID id,
        UUID sessionId,
        Integer overallScore,
        Integer technicalScore,
        Integer behaviouralScore,
        Integer communicationScore,
        Integer problemSolvingScore,
        List<String> strongAreas,
        List<String> improvementAreas,
        String detailedFeedback,
        List<RecommendedResource> recommendedResources,
        LocalDateTime generatedAt
) {
    /**
     * A single learning resource recommended to the candidate.
     */
    public record RecommendedResource(String title, String url, String type) {
    }
}
