package com.careerai.resume.service;

import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.UUID;

/**
 * Stores and retrieves resume files in object storage (S3 / LocalStack).
 */
public interface FileStorageService {

    /**
     * Validates and uploads a resume file for the given user.
     *
     * @throws com.careerai.resume.exception.FileProcessingException if the file is invalid or upload fails
     */
    S3UploadResult uploadFile(MultipartFile file, UUID userId);

    /** Deletes the object with the given key (no-op if it does not exist). */
    void deleteFile(String s3Key);

    /** @return a time-limited presigned GET URL for the given object key. */
    String generatePresignedUrl(String s3Key, Duration expiry);
}
