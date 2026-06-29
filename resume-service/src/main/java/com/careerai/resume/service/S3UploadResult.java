package com.careerai.resume.service;

/**
 * Outcome of storing a resume file in object storage.
 *
 * @param key         the S3 object key
 * @param url         a (non-presigned) URL identifying the stored object
 * @param contentType the stored content type
 * @param sizeBytes   the stored object size in bytes
 */
public record S3UploadResult(String key, String url, String contentType, long sizeBytes) {
}
