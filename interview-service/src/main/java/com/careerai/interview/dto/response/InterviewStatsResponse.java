package com.careerai.interview.dto.response;

/**
 * Aggregate interview statistics for a user.
 */
public record InterviewStatsResponse(
        long totalSessions,
        long completedSessions,
        double completionRate,
        Double averageScore
) {
}
