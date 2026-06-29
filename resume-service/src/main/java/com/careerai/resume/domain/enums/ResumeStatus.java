package com.careerai.resume.domain.enums;

/**
 * Lifecycle of an uploaded resume as it moves through storage, parsing and AI analysis.
 */
public enum ResumeStatus {
    UPLOADED,
    PROCESSING,
    ANALYSED,
    FAILED
}
