package com.careerai.jobmatch.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event published by resume-service on the {@code resume.analysed} topic once a resume has been
 * analysed. Field shape mirrors the producer ({@code com.careerai.resume.event.ResumeAnalysedEvent})
 * so the JSON payload binds directly. job-match-service consumes it to build a resume embedding and
 * compute matches.
 */
public record ResumeAnalysedEvent(
        UUID resumeId,
        UUID userId,
        String userEmail,
        List<String> extractedSkills,
        List<String> targetRoles,
        Integer atsScore,
        LocalDateTime analysedAt) {
}
