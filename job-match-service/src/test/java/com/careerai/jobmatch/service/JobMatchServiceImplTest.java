package com.careerai.jobmatch.service;

import com.careerai.common.exception.ResourceNotFoundException;
import com.careerai.jobmatch.domain.entity.JobListing;
import com.careerai.jobmatch.domain.entity.JobMatch;
import com.careerai.jobmatch.domain.entity.ResumeEmbedding;
import com.careerai.jobmatch.domain.enums.MatchStatus;
import com.careerai.jobmatch.dto.ai.SkillGapResult;
import com.careerai.jobmatch.dto.response.JobListingResponse;
import com.careerai.jobmatch.dto.response.JobMatchResponse;
import com.careerai.jobmatch.dto.response.SkillGapResponse;
import com.careerai.jobmatch.mapper.JobMatchMapper;
import com.careerai.jobmatch.repository.JobEmbeddingRepository;
import com.careerai.jobmatch.repository.JobListingRepository;
import com.careerai.jobmatch.repository.JobMatchRepository;
import com.careerai.jobmatch.repository.ResumeEmbeddingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobMatchServiceImplTest {

    @Mock private ResumeEmbeddingRepository resumeEmbeddingRepository;
    @Mock private JobMatchRepository jobMatchRepository;
    @Mock private JobListingRepository jobListingRepository;
    @Mock private JobEmbeddingRepository jobEmbeddingRepository;
    @Mock private EmbeddingService embeddingService;
    @Mock private SkillGapService skillGapService;
    @Mock private JobMatchMapper mapper;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JobMatchServiceImpl service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID RESUME_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new JobMatchServiceImpl(resumeEmbeddingRepository, jobMatchRepository, jobListingRepository,
                jobEmbeddingRepository, embeddingService, skillGapService, mapper, objectMapper, redisTemplate, 30);
    }

    @Test
    void findMatches_cacheHit_returnsWithoutDbCall() throws Exception {
        JobMatchResponse cached = sampleResponse();
        when(valueOperations.get("matches:" + USER_ID + ":" + RESUME_ID))
                .thenReturn(objectMapper.writeValueAsString(List.of(cached)));

        List<JobMatchResponse> result = service.findMatchesForResume(RESUME_ID, USER_ID, 10);

        assertThat(result).hasSize(1);
        verify(resumeEmbeddingRepository, never()).findByResumeId(any());
        verify(jobMatchRepository, never()).findSimilarJobs(anyString(), eq(10));
    }

    @Test
    void findMatches_cacheMiss_computesPersistsAndCaches() {
        when(valueOperations.get(anyString())).thenReturn(null);

        ResumeEmbedding embedding = ResumeEmbedding.builder()
                .resumeId(RESUME_ID).userId(USER_ID)
                .embedding(new float[]{0.1f, 0.2f, 0.3f})
                .extractedSkills("[\"Java\",\"Spring\"]")
                .build();
        when(resumeEmbeddingRepository.findByResumeId(RESUME_ID)).thenReturn(Optional.of(embedding));

        UUID jobId = UUID.randomUUID();
        JobListing listing = JobListing.builder()
                .id(jobId).title("Backend Engineer").company("Acme")
                .descriptionText("desc").requiredSkills("[\"Java\",\"Kafka\"]").build();
        when(jobMatchRepository.findSimilarJobs(anyString(), eq(5)))
                .thenReturn(List.<Object[]>of(new Object[]{jobId, 0.91d}));
        when(jobListingRepository.findById(jobId)).thenReturn(Optional.of(listing));
        when(jobMatchRepository.findByUserIdAndResumeIdAndJobListingId(USER_ID, RESUME_ID, jobId))
                .thenReturn(Optional.empty());
        when(jobMatchRepository.save(any(JobMatch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toJobMatchResponse(any(JobMatch.class))).thenReturn(sampleResponse());

        List<JobMatchResponse> result = service.findMatchesForResume(RESUME_ID, USER_ID, 5);

        assertThat(result).hasSize(1);
        verify(jobMatchRepository).save(any(JobMatch.class));
        verify(valueOperations).set(eq("matches:" + USER_ID + ":" + RESUME_ID), anyString(), any());
    }

    @Test
    void findMatches_noResumeEmbedding_throws() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(resumeEmbeddingRepository.findByResumeId(RESUME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findMatchesForResume(RESUME_ID, USER_ID, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void saveJob_persistsListingAndEmbedding() {
        JobListing listing = JobListing.builder()
                .title("ML Engineer").company("OpenAI").descriptionText("train models")
                .requiredSkills("[\"Python\"]").build();
        when(jobListingRepository.save(listing)).thenAnswer(inv -> {
            JobListing l = inv.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{0.5f, 0.5f});

        JobListing saved = service.saveJob(listing);

        assertThat(saved.getId()).isNotNull();
        verify(jobEmbeddingRepository).save(any());
    }

    @Test
    void getMatchById_notOwned_throws() {
        UUID matchId = UUID.randomUUID();
        when(jobMatchRepository.findByIdAndUserId(matchId, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMatchById(matchId, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateMatchStatus_updatesAndReturns() {
        UUID matchId = UUID.randomUUID();
        JobMatch match = JobMatch.builder().id(matchId).userId(USER_ID).status(MatchStatus.PENDING_REVIEW).build();
        when(jobMatchRepository.findByIdAndUserId(matchId, USER_ID)).thenReturn(Optional.of(match));
        when(jobMatchRepository.save(match)).thenReturn(match);
        when(mapper.toJobMatchResponse(match)).thenReturn(sampleResponse());

        service.updateMatchStatus(matchId, MatchStatus.APPLIED, USER_ID);

        assertThat(match.getStatus()).isEqualTo(MatchStatus.APPLIED);
        verify(jobMatchRepository).save(match);
    }

    @Test
    void analyseSkillGap_callsClaudeAndMaps() {
        UUID matchId = UUID.randomUUID();
        JobListing listing = JobListing.builder().id(UUID.randomUUID()).title("Backend Engineer")
                .requiredSkills("[\"Java\",\"Kafka\"]").build();
        JobMatch match = JobMatch.builder().id(matchId).userId(USER_ID).resumeId(RESUME_ID).jobListing(listing).build();
        when(jobMatchRepository.findByIdAndUserId(matchId, USER_ID)).thenReturn(Optional.of(match));
        when(resumeEmbeddingRepository.findByResumeId(RESUME_ID)).thenReturn(Optional.of(
                ResumeEmbedding.builder().extractedSkills("[\"Java\"]").build()));
        SkillGapResult result = new SkillGapResult(List.of("Java"), List.of("Kafka"), List.of(), 60,
                "NEEDS_WORK", List.of(), "summary");
        when(skillGapService.analyseSkillGap(any(), any(), eq("Backend Engineer"))).thenReturn(result);
        SkillGapResponse mapped = new SkillGapResponse("Backend Engineer", List.of("Java"), List.of("Kafka"),
                List.of(), 60, "NEEDS_WORK", "summary");
        when(mapper.toSkillGapResponse("Backend Engineer", result)).thenReturn(mapped);

        SkillGapResponse response = service.analyseSkillGap(matchId, USER_ID);

        assertThat(response.gapScore()).isEqualTo(60);
        verify(skillGapService).analyseSkillGap(List.of("Java"), List.of("Java", "Kafka"), "Backend Engineer");
    }

    @Test
    void getMyMatches_noStatus_usesUnfilteredQuery() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        JobMatch match = JobMatch.builder().id(UUID.randomUUID()).userId(USER_ID).build();
        when(jobMatchRepository.findByUserId(USER_ID, pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(match)));
        when(mapper.toJobMatchResponse(any(JobMatch.class))).thenReturn(sampleResponse());

        var page = service.getMyMatches(USER_ID, null, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        verify(jobMatchRepository).findByUserId(USER_ID, pageable);
    }

    @Test
    void getMyMatches_withStatus_usesFilteredQuery() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        when(jobMatchRepository.findByUserIdAndStatus(USER_ID, MatchStatus.SAVED, pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        service.getMyMatches(USER_ID, MatchStatus.SAVED, pageable);

        verify(jobMatchRepository).findByUserIdAndStatus(USER_ID, MatchStatus.SAVED, pageable);
    }

    @Test
    void searchJobs_blankFiltersBecomeNull() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        when(jobListingRepository.search(eq(null), eq(null), eq(pageable)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        service.searchJobs("  ", "", pageable);

        verify(jobListingRepository).search(null, null, pageable);
    }

    @Test
    void getLearningPath_buildsFromClaudeResult() {
        UUID matchId = UUID.randomUUID();
        JobListing listing = JobListing.builder().id(UUID.randomUUID()).title("Backend Engineer")
                .requiredSkills("[\"Java\"]").build();
        JobMatch match = JobMatch.builder().id(matchId).userId(USER_ID).resumeId(RESUME_ID)
                .jobListing(listing).build();
        when(jobMatchRepository.findByIdAndUserId(matchId, USER_ID)).thenReturn(Optional.of(match));
        when(resumeEmbeddingRepository.findByResumeId(RESUME_ID)).thenReturn(Optional.empty());
        SkillGapResult result = new SkillGapResult(List.of(), List.of(), List.of(), 50, "NEEDS_WORK",
                List.of(), "summary");
        when(skillGapService.analyseSkillGap(any(), any(), eq("Backend Engineer"))).thenReturn(result);
        when(mapper.toLearningPathResponse(eq("Backend Engineer"), eq(result)))
                .thenReturn(new com.careerai.jobmatch.dto.response.LearningPathResponse("Backend Engineer", 8, List.of()));

        var response = service.getLearningPath(matchId, USER_ID);

        assertThat(response.totalEstimatedWeeks()).isEqualTo(8);
    }

    private JobMatchResponse sampleResponse() {
        JobListingResponse job = new JobListingResponse(UUID.randomUUID(), "Backend Engineer", "Acme", "Remote",
                "REMOTE", "desc", List.of("Java"), List.of(), "$100k", "SENIOR", "http://x", null);
        return new JobMatchResponse(UUID.randomUUID(), job, 0.9, 90, List.of("Java"), List.of("Kafka"),
                "PENDING_REVIEW", null);
    }
}
