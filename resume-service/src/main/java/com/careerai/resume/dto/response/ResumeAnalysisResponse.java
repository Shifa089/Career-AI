package com.careerai.resume.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Public view of a {@link com.careerai.resume.domain.entity.ResumeAnalysis}, with JSON-array
 * columns expanded back into lists.
 */
public record ResumeAnalysisResponse(
        UUID id,
        UUID resumeId,
        Integer atsScore,
        String summary,
        Integer yearsOfExperience,
        String educationLevel,
        List<String> targetRoles,
        List<String> strengths,
        List<String> weaknesses,
        List<String> suggestions,
        List<String> keywords,
        List<String> missingKeywords,
        List<SkillExtractionResponse> skills,
        LocalDateTime createdAt
) {
}
