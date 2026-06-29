package com.careerai.resume.service;

import com.careerai.resume.dto.response.ResumeAnalysisResponse;
import com.careerai.resume.dto.response.ResumeResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Orchestrates resume upload, storage, text extraction, AI analysis and retrieval.
 */
public interface ResumeService {

    /**
     * Stores and indexes a resume, then triggers asynchronous AI analysis. Returns as soon as the
     * file is stored (status {@code PROCESSING}); analysis completes in the background.
     */
    ResumeResponse uploadAndAnalyse(MultipartFile file, UUID userId, String userEmail, String targetRole);

    /** @return the user's resumes, newest first. */
    List<ResumeResponse> getResumesByUser(UUID userId);

    /** @return a single resume owned by the user. */
    ResumeResponse getResume(UUID resumeId, UUID requestingUserId);

    /** @return the analysis for a resume owned by the user (Redis cache first, then DB). */
    ResumeAnalysisResponse getAnalysis(UUID resumeId, UUID requestingUserId);

    /** Deletes a resume (and its stored file) owned by the user. */
    void deleteResume(UUID resumeId, UUID requestingUserId);

    /** Marks the given resume as the user's primary, clearing the flag on the rest. */
    ResumeResponse setPrimary(UUID resumeId, UUID userId);

    /** @return a time-limited presigned download URL for a resume owned by the user. */
    String getDownloadUrl(UUID resumeId, UUID requestingUserId);
}
