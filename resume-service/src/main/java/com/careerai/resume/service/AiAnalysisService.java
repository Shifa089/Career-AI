package com.careerai.resume.service;

import com.careerai.resume.dto.ai.ResumeAnalysisResult;

/**
 * Produces a structured analysis of a resume using an AI model.
 */
public interface AiAnalysisService {

    /**
     * Analyses the given resume text, optionally tailored to a target role.
     *
     * @throws com.careerai.resume.exception.AiAnalysisException if the model call or parsing fails
     */
    ResumeAnalysisResult analyseResume(String resumeText, String targetRole);
}
