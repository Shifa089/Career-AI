package com.careerai.resume.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Optional metadata accompanying a resume upload. The file itself is sent as a multipart part.
 *
 * @param targetRole the job title the candidate is targeting, used to tailor the analysis
 */
public record ResumeUploadRequest(
        @Size(max = 150, message = "targetRole must be at most 150 characters")
        String targetRole
) {
}
