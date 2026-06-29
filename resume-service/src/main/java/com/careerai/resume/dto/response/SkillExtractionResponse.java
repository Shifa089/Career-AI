package com.careerai.resume.dto.response;

/**
 * Public view of a single AI-extracted skill.
 */
public record SkillExtractionResponse(
        String skillName,
        String category,
        String proficiencyLevel,
        Integer yearsUsed,
        boolean inferredFromContext
) {
}
