package com.careerai.jobmatch.dto.response;

import java.util.List;

/**
 * A prioritised learning plan to close a candidate's skill gap for a specific job.
 */
public record LearningPathResponse(
        String jobTitle,
        Integer totalEstimatedWeeks,
        List<PrioritisedSkill> prioritizedSkills) {

    /**
     * One skill to learn, with its priority, rough effort and curated resources.
     */
    public record PrioritisedSkill(
            String skill,
            String priority,
            Integer estimatedWeeks,
            List<Resource> resources) {
    }

    /**
     * A learning resource for a skill.
     */
    public record Resource(String title, String url, String type) {
    }
}
