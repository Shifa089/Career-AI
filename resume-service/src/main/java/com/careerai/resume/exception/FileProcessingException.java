package com.careerai.resume.exception;

import com.careerai.common.exception.BadRequestException;

/**
 * Thrown when an uploaded file is rejected or cannot be processed (invalid type, too large,
 * unreadable, or storage failure). Handled centrally as HTTP 400.
 */
public class FileProcessingException extends BadRequestException {

    public FileProcessingException(String message) {
        super(message);
    }
}
