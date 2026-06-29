package com.careerai.resume.dto.response;

import com.careerai.resume.domain.enums.ResumeStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Public view of a {@link com.careerai.resume.domain.entity.Resume}.
 */
public record ResumeResponse(
        UUID id,
        UUID userId,
        String userEmail,
        String originalFileName,
        String contentType,
        Long fileSizeBytes,
        ResumeStatus status,
        boolean primary,
        Integer version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime analysedAt
) {
}
