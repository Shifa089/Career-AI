package com.careerai.resume.service;

import com.careerai.resume.config.AsyncConfig;
import com.careerai.resume.config.KafkaConfig;
import com.careerai.resume.domain.entity.Resume;
import com.careerai.resume.domain.entity.ResumeAnalysis;
import com.careerai.resume.domain.entity.SkillExtraction;
import com.careerai.resume.domain.enums.ResumeStatus;
import com.careerai.resume.dto.ai.ResumeAnalysisResult;
import com.careerai.resume.dto.response.ResumeAnalysisResponse;
import com.careerai.resume.event.ResumeAnalysedEvent;
import com.careerai.resume.mapper.ResumeMapper;
import com.careerai.resume.repository.ResumeAnalysisRepository;
import com.careerai.resume.repository.ResumeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Runs the slow part of the pipeline off the request thread: AI analysis, persistence, caching and
 * event publication. Kept in a separate bean so the {@link Async} proxy is applied when invoked
 * from {@link ResumeServiceImpl}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AsyncResumeAnalyser {

    static final Duration ANALYSIS_CACHE_TTL = Duration.ofHours(1);

    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository analysisRepository;
    private final AiAnalysisService aiAnalysisService;
    private final ResumeMapper resumeMapper;
    private final RedisTemplate<String, ResumeAnalysisResponse> analysisRedisTemplate;
    private final KafkaTemplate<UUID, ResumeAnalysedEvent> resumeKafkaTemplate;
    private final ObjectMapper objectMapper;

    @Async(AsyncConfig.ANALYSIS_EXECUTOR)
    @Transactional
    public CompletableFuture<Void> analyse(UUID resumeId, String targetRole) {
        Resume resume = resumeRepository.findById(resumeId).orElse(null);
        if (resume == null) {
            log.warn("Resume {} disappeared before analysis could run", resumeId);
            return CompletableFuture.completedFuture(null);
        }

        try {
            ResumeAnalysisResult result = aiAnalysisService.analyseResume(resume.getExtractedText(), targetRole);

            ResumeAnalysis analysis = toAnalysisEntity(resume, result);
            analysisRepository.save(analysis);

            resume.setStatus(ResumeStatus.ANALYSED);
            resume.setAnalysedAt(LocalDateTime.now());
            resumeRepository.save(resume);

            cache(resume.getUserId(), resumeId, resumeMapper.toAnalysisResponse(analysis));
            publish(resume, result);

            log.info("Resume {} analysed (atsScore={})", resumeId, result.atsScore());
        } catch (Exception e) {
            log.error("Analysis failed for resume {}; marking FAILED", resumeId, e);
            resume.setStatus(ResumeStatus.FAILED);
            resumeRepository.save(resume);
        }
        return CompletableFuture.completedFuture(null);
    }

    private ResumeAnalysis toAnalysisEntity(Resume resume, ResumeAnalysisResult result) {
        ResumeAnalysis analysis = ResumeAnalysis.builder()
                .resume(resume)
                .atsScore(result.atsScore())
                .summary(result.summary())
                .yearsOfExperience(result.yearsOfExperience())
                .educationLevel(result.educationLevel())
                .targetRoles(toJson(result.targetRoles()))
                .strengths(toJson(result.strengths()))
                .weaknesses(toJson(result.weaknesses()))
                .suggestions(toJson(result.suggestions()))
                .keywords(toJson(result.keywords()))
                .missingKeywords(toJson(result.missingKeywords()))
                .build();

        if (result.skills() != null) {
            for (ResumeAnalysisResult.ExtractedSkill skill : result.skills()) {
                analysis.addSkill(SkillExtraction.builder()
                        .skillName(skill.skillName())
                        .category(skill.category())
                        .proficiencyLevel(skill.proficiencyLevel())
                        .yearsUsed(skill.yearsUsed())
                        .inferredFromContext(Boolean.TRUE.equals(skill.inferredFromContext()))
                        .build());
            }
        }
        return analysis;
    }

    private void cache(UUID userId, UUID resumeId, ResumeAnalysisResponse response) {
        try {
            analysisRedisTemplate.opsForValue()
                    .set(ResumeServiceImpl.analysisCacheKey(userId, resumeId), response, ANALYSIS_CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache analysis for resume {}: {}", resumeId, e.getMessage());
        }
    }

    /**
     * Publishes the {@code resume.analysed} event. A broker outage must never fail the analysis
     * itself — the analysis and cache are already persisted by the time we get here — so any Kafka
     * error is logged and swallowed rather than propagated (which would mark the resume FAILED).
     */
    private void publish(Resume resume, ResumeAnalysisResult result) {
        try {
            List<String> skillNames = result.skills() == null
                    ? List.of()
                    : result.skills().stream().map(ResumeAnalysisResult.ExtractedSkill::skillName).toList();
            ResumeAnalysedEvent event = new ResumeAnalysedEvent(
                    resume.getId(),
                    resume.getUserId(),
                    resume.getUserEmail(),
                    skillNames,
                    result.targetRoles() == null ? List.of() : result.targetRoles(),
                    result.atsScore(),
                    resume.getAnalysedAt());
            resumeKafkaTemplate.send(KafkaConfig.RESUME_ANALYSED_TOPIC, resume.getId(), event);
        } catch (Exception e) {
            log.warn("Failed to publish resume.analysed event for resume {} (analysis still succeeded): {}",
                    resume.getId(), e.getMessage());
        }
    }

    private String toJson(List<String> values) {
        if (values == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize list to JSON; storing empty array", e);
            return "[]";
        }
    }
}
