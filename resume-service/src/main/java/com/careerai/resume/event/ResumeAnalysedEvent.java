package com.careerai.resume.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Domain event published to the {@code resume.analysed} Kafka topic once a resume has been
 * successfully analysed. Consumed by downstream services (e.g. job-match) for indexing.
 */
public record ResumeAnalysedEvent(
        UUID resumeId,
        UUID userId,
        String userEmail,
        List<String> extractedSkills,
        List<String> targetRoles,
        Integer atsScore,
        LocalDateTime analysedAt
) {
}
