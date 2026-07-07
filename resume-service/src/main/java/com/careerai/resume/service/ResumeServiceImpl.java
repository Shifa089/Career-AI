package com.careerai.resume.service;

import com.careerai.common.exception.UnauthorizedException;
import com.careerai.resume.domain.entity.Resume;
import com.careerai.resume.domain.entity.ResumeAnalysis;
import com.careerai.resume.domain.enums.ResumeStatus;
import com.careerai.resume.dto.response.ResumeAnalysisResponse;
import com.careerai.resume.dto.response.ResumeResponse;
import com.careerai.resume.exception.FileProcessingException;
import com.careerai.resume.exception.ResumeNotFoundException;
import com.careerai.resume.mapper.ResumeMapper;
import com.careerai.resume.repository.ResumeAnalysisRepository;
import com.careerai.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Default {@link ResumeService}. Performs storage + text extraction synchronously and delegates the
 * AI analysis to {@link AsyncResumeAnalyser} so the upload request returns immediately.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(15);

    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository analysisRepository;
    private final FileStorageService fileStorageService;
    private final TextExtractionService textExtractionService;
    private final AsyncResumeAnalyser asyncResumeAnalyser;
    private final ResumeMapper resumeMapper;
    private final RedisTemplate<String, ResumeAnalysisResponse> analysisRedisTemplate;

    @Override
    @Transactional
    public ResumeResponse uploadAndAnalyse(MultipartFile file, UUID userId, String userEmail, String targetRole) {
        S3UploadResult upload = fileStorageService.uploadFile(file, userId);

        String extractedText;
        try {
            extractedText = textExtractionService.extractText(file.getInputStream(), file.getContentType());
        } catch (IOException e) {
            fileStorageService.deleteFile(upload.key());
            throw new FileProcessingException("Could not read uploaded file for text extraction");
        }

        boolean firstResume = resumeRepository.findByUserIdOrderByCreatedAtDesc(userId).isEmpty();

        Resume resume = Resume.builder()
                .userId(userId)
                .userEmail(userEmail)
                .originalFileName(file.getOriginalFilename())
                .s3Key(upload.key())
                .s3Url(upload.url())
                .contentType(upload.contentType())
                .fileSizeBytes(upload.sizeBytes())
                .status(ResumeStatus.PROCESSING)
                .extractedText(extractedText)
                .isPrimary(firstResume)
                .version(1)
                .build();
        resume = resumeRepository.save(resume);

        // Kick off AI analysis only AFTER this transaction commits. The analyser is @Async +
        // @Transactional, so it runs on its own thread/transaction; if invoked before commit it
        // cannot see the just-saved resume row ("disappeared before analysis could run") and the
        // resume would sit in PROCESSING forever.
        final UUID resumeId = resume.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    asyncResumeAnalyser.analyse(resumeId, targetRole);
                }
            });
        } else {
            asyncResumeAnalyser.analyse(resumeId, targetRole);
        }

        log.info("Resume {} uploaded for user {} ({} bytes); analysis queued",
                resume.getId(), userId, upload.sizeBytes());
        return resumeMapper.toResumeResponse(resume);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponse> getResumesByUser(UUID userId) {
        return resumeRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(resumeMapper::toResumeResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponse getResume(UUID resumeId, UUID requestingUserId) {
        return resumeMapper.toResumeResponse(loadOwned(resumeId, requestingUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeAnalysisResponse getAnalysis(UUID resumeId, UUID requestingUserId) {
        String key = analysisCacheKey(requestingUserId, resumeId);
        ResumeAnalysisResponse cached = analysisRedisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        ResumeAnalysis analysis = analysisRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new ResumeNotFoundException(resumeId));
        if (!analysis.getResume().getUserId().equals(requestingUserId)) {
            throw new UnauthorizedException("You do not have access to this resume");
        }

        ResumeAnalysisResponse response = resumeMapper.toAnalysisResponse(analysis);
        analysisRedisTemplate.opsForValue().set(key, response, AsyncResumeAnalyser.ANALYSIS_CACHE_TTL);
        return response;
    }

    @Override
    @Transactional
    public void deleteResume(UUID resumeId, UUID requestingUserId) {
        Resume resume = loadOwned(resumeId, requestingUserId);
        fileStorageService.deleteFile(resume.getS3Key());
        resumeRepository.delete(resume);
        analysisRedisTemplate.delete(analysisCacheKey(requestingUserId, resumeId));
        log.info("Resume {} deleted for user {}", resumeId, requestingUserId);
    }

    @Override
    @Transactional
    public ResumeResponse setPrimary(UUID resumeId, UUID userId) {
        Resume target = loadOwned(resumeId, userId);
        List<Resume> resumes = resumeRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (Resume resume : resumes) {
            resume.setPrimary(resume.getId().equals(resumeId));
        }
        resumeRepository.saveAll(resumes);
        target.setPrimary(true);
        return resumeMapper.toResumeResponse(target);
    }

    @Override
    @Transactional(readOnly = true)
    public String getDownloadUrl(UUID resumeId, UUID requestingUserId) {
        Resume resume = loadOwned(resumeId, requestingUserId);
        return fileStorageService.generatePresignedUrl(resume.getS3Key(), DOWNLOAD_URL_TTL);
    }

    private Resume loadOwned(UUID resumeId, UUID requestingUserId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResumeNotFoundException(resumeId));
        if (!resume.getUserId().equals(requestingUserId)) {
            throw new UnauthorizedException("You do not have access to this resume");
        }
        return resume;
    }

    /** Cache key namespaced by user so a cache hit implies ownership. */
    static String analysisCacheKey(UUID userId, UUID resumeId) {
        return "analysis:" + userId + ":" + resumeId;
    }
}
