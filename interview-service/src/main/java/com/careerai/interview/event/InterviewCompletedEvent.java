package com.careerai.interview.event;

import com.careerai.interview.domain.enums.InterviewType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Domain event published to the {@code interview.completed} Kafka topic once a session has been
 * fully scored. Consumed by downstream services (e.g. job-match) for candidate profiling.
 */
public record InterviewCompletedEvent(
        UUID sessionId,
        UUID userId,
        String jobTitle,
        InterviewType type,
        Integer overallScore,
        Integer totalQuestions,
        List<String> skillsAssessed,
        LocalDateTime completedAt
) {
}
