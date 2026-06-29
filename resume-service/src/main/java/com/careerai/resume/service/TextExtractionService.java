package com.careerai.resume.service;

import java.io.InputStream;
import java.util.Map;

/**
 * Extracts plain text and metadata from resume documents (PDF/DOCX/TXT).
 */
public interface TextExtractionService {

    /**
     * Extracts and normalizes the text content of a document.
     *
     * @throws com.careerai.resume.exception.FileProcessingException if the document cannot be parsed
     */
    String extractText(InputStream input, String contentType);

    /** @return document metadata (author, created date, page count, etc.) as a string map. */
    Map<String, String> extractMetadata(InputStream input);

    /** @return whether the given content type is supported for extraction. */
    boolean isSupported(String contentType);
}
