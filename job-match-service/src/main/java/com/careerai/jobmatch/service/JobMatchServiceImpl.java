package com.careerai.jobmatch.service;

import com.careerai.common.exception.ResourceNotFoundException;
import com.careerai.jobmatch.domain.entity.JobEmbedding;
import com.careerai.jobmatch.domain.entity.JobListing;
import com.careerai.jobmatch.domain.entity.JobMatch;
import com.careerai.jobmatch.domain.entity.ResumeEmbedding;
import com.careerai.jobmatch.domain.enums.MatchStatus;
import com.careerai.jobmatch.domain.type.PgVectorType;
import com.careerai.jobmatch.dto.ai.SkillGapResult;
import com.careerai.jobmatch.dto.response.JobListingResponse;
import com.careerai.jobmatch.dto.response.JobMatchResponse;
import com.careerai.jobmatch.dto.response.LearningPathResponse;
import com.careerai.jobmatch.dto.response.SkillGapResponse;
import com.careerai.jobmatch.mapper.JobMatchMapper;
import com.careerai.jobmatch.repository.JobEmbeddingRepository;
import com.careerai.jobmatch.repository.JobListingRepository;
import com.careerai.jobmatch.repository.JobMatchRepository;
import com.careerai.jobmatch.repository.ResumeEmbeddingRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RAG matching pipeline implementation. Resume and job embeddings are compared with pgvector cosine
 * similarity (native query), results are materialised as {@link JobMatch} rows and cached in Redis,
 * and skill-gap / learning-path views are produced on demand by Claude.
 */
@Service
@Slf4j
public class JobMatchServiceImpl implements JobMatchService {

    private static final TypeReference<List<JobMatchResponse>> MATCH_LIST = new TypeReference<>() {
    };

    private final ResumeEmbeddingRepository resumeEmbeddingRepository;
    private final JobMatchRepository jobMatchRepository;
    private final JobListingRepository jobListingRepository;
    private final JobEmbeddingRepository jobEmbeddingRepository;
    private final EmbeddingService embeddingService;
    private final SkillGapService skillGapService;
    private final JobMatchMapper mapper;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final Duration cacheTtl;

    public JobMatchServiceImpl(ResumeEmbeddingRepository resumeEmbeddingRepository,
                               JobMatchRepository jobMatchRepository,
                               JobListingRepository jobListingRepository,
                               JobEmbeddingRepository jobEmbeddingRepository,
                               EmbeddingService embeddingService,
                               SkillGapService skillGapService,
                               JobMatchMapper mapper,
                               ObjectMapper objectMapper,
                               StringRedisTemplate redisTemplate,
                               @Value("${job-match.matching.cache-ttl-minutes:30}") long cacheTtlMinutes) {
        this.resumeEmbeddingRepository = resumeEmbeddingRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.jobListingRepository = jobListingRepository;
        this.jobEmbeddingRepository = jobEmbeddingRepository;
        this.embeddingService = embeddingService;
        this.skillGapService = skillGapService;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.cacheTtl = Duration.ofMinutes(cacheTtlMinutes);
    }

    @Override
    @Transactional
    public List<JobMatchResponse> findMatchesForResume(UUID resumeId, UUID userId, int limit) {
        String cacheKey = matchesCacheKey(userId, resumeId);
        List<JobMatchResponse> cached = readCache(cacheKey);
        if (cached != null) {
            log.debug("Returning {} cached matches for resume {}", cached.size(), resumeId);
            return cached;
        }

        ResumeEmbedding resumeEmbedding = resumeEmbeddingRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("ResumeEmbedding", resumeId));
        if (resumeEmbedding.getEmbedding() == null) {
            throw new ResourceNotFoundException("Resume embedding has not been generated for resume " + resumeId);
        }

        Set<String> resumeSkills = normalise(parseSkills(resumeEmbedding.getExtractedSkills()));
        String embeddingLiteral = PgVectorType.format(resumeEmbedding.getEmbedding());

        List<Object[]> rows = jobMatchRepository.findSimilarJobs(embeddingLiteral, limit);
        List<JobMatchResponse> responses = new ArrayList<>(rows.size());

        for (Object[] row : rows) {
            UUID jobListingId = toUuid(row[0]);
            double similarity = ((Number) row[1]).doubleValue();

            JobListing listing = jobListingRepository.findById(jobListingId).orElse(null);
            if (listing == null) {
                continue;
            }

            List<String> required = parseSkills(listing.getRequiredSkills());
            List<String> matched = required.stream()
                    .filter(skill -> resumeSkills.contains(skill.toLowerCase()))
                    .toList();
            List<String> missing = required.stream()
                    .filter(skill -> !resumeSkills.contains(skill.toLowerCase()))
                    .toList();

            JobMatch match = jobMatchRepository
                    .findByUserIdAndResumeIdAndJobListingId(userId, resumeId, jobListingId)
                    .orElseGet(() -> JobMatch.builder()
                            .userId(userId)
                            .resumeId(resumeId)
                            .jobListing(listing)
                            .status(MatchStatus.PENDING_REVIEW)
                            .build());
            match.setSimilarityScore(similarity);
            match.setMatchPercentage(toPercentage(similarity));
            match.setMatchedSkills(toJson(matched));
            match.setMissingSkills(toJson(missing));

            responses.add(mapper.toJobMatchResponse(jobMatchRepository.save(match)));
        }

        writeCache(cacheKey, responses);
        log.info("Computed {} matches for resume {} (user {})", responses.size(), resumeId, userId);
        return responses;
    }

    @Override
    @Transactional
    public JobListing saveJob(JobListing listing) {
        JobListing saved = jobListingRepository.save(listing);

        StringBuilder text = new StringBuilder(saved.getTitle()).append(". ").append(saved.getDescriptionText());
        List<String> skills = parseSkills(saved.getRequiredSkills());
        if (!skills.isEmpty()) {
            text.append(" Required skills: ").append(String.join(", ", skills));
        }

        float[] embedding = embeddingService.generateEmbedding(text.toString());
        jobEmbeddingRepository.save(JobEmbedding.builder()
                .jobListing(saved)
                .embedding(embedding)
                .build());
        log.info("Saved job '{}' at {} with embedding", saved.getTitle(), saved.getCompany());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public JobMatchResponse getMatchById(UUID matchId, UUID userId) {
        return mapper.toJobMatchResponse(loadOwned(matchId, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobMatchResponse> getMyMatches(UUID userId, MatchStatus status, Pageable pageable) {
        Page<JobMatch> page = status == null
                ? jobMatchRepository.findByUserId(userId, pageable)
                : jobMatchRepository.findByUserIdAndStatus(userId, status, pageable);
        return page.map(mapper::toJobMatchResponse);
    }

    @Override
    @Transactional
    public JobMatchResponse updateMatchStatus(UUID matchId, MatchStatus status, UUID userId) {
        JobMatch match = loadOwned(matchId, userId);
        match.setStatus(status);
        return mapper.toJobMatchResponse(jobMatchRepository.save(match));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobListingResponse> searchJobs(String keyword, String location, Pageable pageable) {
        String kw = blankToNull(keyword);
        String loc = blankToNull(location);
        // With no filters this is the candidate "Latest Jobs" feed (newest first, no resume needed);
        // with filters it is a newest-first keyword/location search.
        Page<JobListing> page = (kw == null && loc == null)
                ? jobListingRepository.findActiveLatest(pageable)
                : jobListingRepository.search(kw, loc, pageable);
        return page.map(mapper::toJobListingResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SkillGapResponse analyseSkillGap(UUID matchId, UUID userId) {
        JobMatch match = loadOwned(matchId, userId);
        SkillGapResult result = runSkillGap(match);
        return mapper.toSkillGapResponse(match.getJobListing().getTitle(), result);
    }

    @Override
    @Transactional(readOnly = true)
    public LearningPathResponse getLearningPath(UUID matchId, UUID userId) {
        JobMatch match = loadOwned(matchId, userId);
        SkillGapResult result = runSkillGap(match);
        return mapper.toLearningPathResponse(match.getJobListing().getTitle(), result);
    }

    private SkillGapResult runSkillGap(JobMatch match) {
        List<String> resumeSkills = resumeEmbeddingRepository.findByResumeId(match.getResumeId())
                .map(re -> parseSkills(re.getExtractedSkills()))
                .orElseGet(List::of);
        List<String> required = parseSkills(match.getJobListing().getRequiredSkills());
        return skillGapService.analyseSkillGap(resumeSkills, required, match.getJobListing().getTitle());
    }

    private JobMatch loadOwned(UUID matchId, UUID userId) {
        return jobMatchRepository.findByIdAndUserId(matchId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("JobMatch", matchId));
    }

    // --- caching helpers -----------------------------------------------------

    private String matchesCacheKey(UUID userId, UUID resumeId) {
        return "matches:" + userId + ":" + resumeId;
    }

    private List<JobMatchResponse> readCache(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            return json == null ? null : objectMapper.readValue(json, MATCH_LIST);
        } catch (Exception e) {
            log.warn("Failed to read match cache {}: {}", key, e.getMessage());
            return null;
        }
    }

    private void writeCache(String key, List<JobMatchResponse> responses) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(responses), cacheTtl);
        } catch (Exception e) {
            log.warn("Failed to write match cache {}: {}", key, e.getMessage());
        }
    }

    // --- skill helpers -------------------------------------------------------

    private List<String> parseSkills(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
            return values == null ? List.of() : values;
        } catch (Exception e) {
            return List.of();
        }
    }

    private Set<String> normalise(List<String> skills) {
        return skills.stream()
                .filter(StringUtils::hasText)
                .map(s -> s.trim().toLowerCase())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            return "[]";
        }
    }

    private int toPercentage(double similarity) {
        int pct = (int) Math.round(similarity * 100);
        return Math.max(0, Math.min(100, pct));
    }

    private UUID toUuid(Object value) {
        return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
