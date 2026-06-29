package com.careerai.resume.service;

import com.careerai.common.exception.UnauthorizedException;
import com.careerai.resume.domain.entity.Resume;
import com.careerai.resume.domain.entity.ResumeAnalysis;
import com.careerai.resume.domain.enums.ResumeStatus;
import com.careerai.resume.dto.response.ResumeAnalysisResponse;
import com.careerai.resume.dto.response.ResumeResponse;
import com.careerai.resume.mapper.ResumeMapper;
import com.careerai.resume.repository.ResumeAnalysisRepository;
import com.careerai.resume.repository.ResumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
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
class ResumeServiceImplTest {

    @Mock private ResumeRepository resumeRepository;
    @Mock private ResumeAnalysisRepository analysisRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private TextExtractionService textExtractionService;
    @Mock private AsyncResumeAnalyser asyncResumeAnalyser;
    @Mock private ResumeMapper resumeMapper;
    @Mock private RedisTemplate<String, ResumeAnalysisResponse> analysisRedisTemplate;
    @Mock private ValueOperations<String, ResumeAnalysisResponse> valueOperations;

    @InjectMocks private ResumeServiceImpl resumeService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "jane@example.com";

    @BeforeEach
    void setUp() {
        when(analysisRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void uploadAndAnalyse_storesExtractsAndQueuesAnalysis() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", "%PDF-1.4 sample".getBytes());

        when(fileStorageService.uploadFile(file, USER_ID))
                .thenReturn(new S3UploadResult("resumes/" + USER_ID + "/x.pdf", "http://s3/x.pdf", "application/pdf", 15));
        when(textExtractionService.extractText(any(), eq("application/pdf"))).thenReturn("extracted resume text");
        when(resumeRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of());
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> {
            Resume r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
        when(resumeMapper.toResumeResponse(any(Resume.class)))
                .thenAnswer(inv -> toResponse(inv.getArgument(0)));

        ResumeResponse response = resumeService.uploadAndAnalyse(file, USER_ID, EMAIL, "Backend Engineer");

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(ResumeStatus.PROCESSING);
        assertThat(response.primary()).isTrue(); // first resume becomes primary
        verify(fileStorageService).uploadFile(file, USER_ID);
        verify(textExtractionService).extractText(any(), eq("application/pdf"));
        verify(asyncResumeAnalyser).analyse(any(UUID.class), eq("Backend Engineer"));
    }

    @Test
    void getAnalysis_cacheHit_returnsWithoutDbCall() {
        UUID resumeId = UUID.randomUUID();
        ResumeAnalysisResponse cached = sampleAnalysisResponse(resumeId);
        when(valueOperations.get(ResumeServiceImpl.analysisCacheKey(USER_ID, resumeId))).thenReturn(cached);

        ResumeAnalysisResponse result = resumeService.getAnalysis(resumeId, USER_ID);

        assertThat(result).isSameAs(cached);
        verify(analysisRepository, never()).findByResumeId(any());
    }

    @Test
    void getAnalysis_cacheMiss_loadsFromDbAndCaches() {
        UUID resumeId = UUID.randomUUID();
        when(valueOperations.get(anyString())).thenReturn(null);

        Resume resume = Resume.builder().id(resumeId).userId(USER_ID).build();
        ResumeAnalysis analysis = ResumeAnalysis.builder().id(UUID.randomUUID()).resume(resume).build();
        when(analysisRepository.findByResumeId(resumeId)).thenReturn(Optional.of(analysis));
        ResumeAnalysisResponse mapped = sampleAnalysisResponse(resumeId);
        when(resumeMapper.toAnalysisResponse(analysis)).thenReturn(mapped);

        ResumeAnalysisResponse result = resumeService.getAnalysis(resumeId, USER_ID);

        assertThat(result).isSameAs(mapped);
        verify(analysisRepository).findByResumeId(resumeId);
        verify(valueOperations).set(eq(ResumeServiceImpl.analysisCacheKey(USER_ID, resumeId)), eq(mapped), any());
    }

    @Test
    void deleteResume_wrongUser_throwsAndSkipsS3Delete() {
        UUID resumeId = UUID.randomUUID();
        Resume resume = Resume.builder().id(resumeId).userId(UUID.randomUUID()).s3Key("k").build();
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(resume));

        assertThatThrownBy(() -> resumeService.deleteResume(resumeId, USER_ID))
                .isInstanceOf(UnauthorizedException.class);

        verify(fileStorageService, never()).deleteFile(anyString());
        verify(resumeRepository, never()).delete(any());
    }

    @Test
    void deleteResume_owner_deletesFileAndRow() {
        UUID resumeId = UUID.randomUUID();
        Resume resume = Resume.builder().id(resumeId).userId(USER_ID).s3Key("resumes/k.pdf").build();
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(resume));

        resumeService.deleteResume(resumeId, USER_ID);

        verify(fileStorageService).deleteFile("resumes/k.pdf");
        verify(resumeRepository).delete(resume);
        verify(analysisRedisTemplate).delete(ResumeServiceImpl.analysisCacheKey(USER_ID, resumeId));
    }

    private static ResumeResponse toResponse(Resume r) {
        return new ResumeResponse(r.getId(), r.getUserId(), r.getUserEmail(), r.getOriginalFileName(),
                r.getContentType(), r.getFileSizeBytes(), r.getStatus(), r.isPrimary(), r.getVersion(),
                r.getCreatedAt(), r.getUpdatedAt(), r.getAnalysedAt());
    }

    private static ResumeAnalysisResponse sampleAnalysisResponse(UUID resumeId) {
        return new ResumeAnalysisResponse(UUID.randomUUID(), resumeId, 85, "Solid candidate", 5, "BACHELOR",
                List.of("Backend Engineer"), List.of("Java"), List.of("No metrics"), List.of("Add metrics"),
                List.of("Spring"), List.of("Kafka"), List.of(), LocalDateTime.now());
    }
}
