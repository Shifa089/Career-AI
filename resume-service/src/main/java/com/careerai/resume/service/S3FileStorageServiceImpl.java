package com.careerai.resume.service;

import com.careerai.resume.exception.FileProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;

/**
 * S3-backed {@link FileStorageService}. Accepts only PDF/DOCX/TXT up to 5MB and stores objects
 * under {@code resumes/{userId}/{uuid}.{ext}}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class S3FileStorageServiceImpl implements FileStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "application/pdf", "pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx",
            "text/plain", "txt");

    private final S3AsyncClient s3AsyncClient;
    private final S3Presigner s3Presigner;

    @Value("${app.aws.s3.bucket:careerai-resumes}")
    private String bucket;

    @Value("${app.aws.s3.endpoint:}")
    private String endpoint;

    @Value("${app.aws.region:us-east-1}")
    private String region;

    @Override
    public S3UploadResult uploadFile(MultipartFile file, UUID userId) {
        validate(file);

        String extension = ALLOWED_TYPES.get(file.getContentType());
        String key = "resumes/%s/%s.%s".formatted(userId, UUID.randomUUID(), extension);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new FileProcessingException("Could not read uploaded file: " + e.getMessage());
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .contentLength((long) bytes.length)
                .contentDisposition("inline; filename=\"%s\"".formatted(sanitize(file.getOriginalFilename())))
                .build();

        try {
            s3AsyncClient.putObject(request, AsyncRequestBody.fromBytes(bytes)).join();
        } catch (CompletionException e) {
            log.error("Failed to upload resume to S3 (key={})", key, e);
            throw new FileProcessingException("Failed to store the uploaded file");
        }

        log.debug("Stored resume in S3: bucket={}, key={}, size={}B", bucket, key, bytes.length);
        return new S3UploadResult(key, buildObjectUrl(key), file.getContentType(), bytes.length);
    }

    @Override
    public void deleteFile(String s3Key) {
        try {
            s3AsyncClient.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(s3Key).build()).join();
        } catch (CompletionException e) {
            log.warn("Failed to delete S3 object (key={}): {}", s3Key, e.getMessage());
        }
    }

    @Override
    public String generatePresignedUrl(String s3Key, Duration expiry) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .getObjectRequest(getObjectRequest)
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileProcessingException("Uploaded file is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new FileProcessingException("File exceeds the maximum allowed size of 5MB");
        }
        if (!ALLOWED_TYPES.containsKey(file.getContentType())) {
            throw new FileProcessingException(
                    "Unsupported file type '%s'. Allowed types: PDF, DOCX, TXT".formatted(file.getContentType()));
        }
    }

    private String buildObjectUrl(String key) {
        if (StringUtils.hasText(endpoint)) {
            return "%s/%s/%s".formatted(endpoint.replaceAll("/$", ""), bucket, key);
        }
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region, key);
    }

    private String sanitize(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "resume";
        }
        return filename.replaceAll("[\"\\r\\n]", "_");
    }
}
