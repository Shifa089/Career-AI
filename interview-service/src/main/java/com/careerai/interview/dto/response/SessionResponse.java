package com.careerai.interview.dto.response;

import com.careerai.interview.domain.enums.InterviewType;
import com.careerai.interview.domain.enums.SessionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Public view of an {@link com.careerai.interview.domain.entity.InterviewSession}.
 */
public record SessionResponse(
        UUID id,
        String jobTitle,
        String targetCompany,
        InterviewType type,
        SessionStatus status,
        Integer totalQuestions,
        Integer questionsAnswered,
        Integer overallScore,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
}
