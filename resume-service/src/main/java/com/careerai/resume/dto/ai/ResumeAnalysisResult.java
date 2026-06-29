package com.careerai.resume.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Deserialization target for the JSON returned by the AI model. Mirrors the structure requested
 * in the analysis prompt. Unknown properties are ignored to tolerate minor model drift.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResumeAnalysisResult(
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
        List<ExtractedSkill> skills
) {

    /**
     * A single skill entry inside the AI result.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExtractedSkill(
            String skillName,
            String category,
            String proficiencyLevel,
            Integer yearsUsed,
            Boolean inferredFromContext
    ) {
    }
}
