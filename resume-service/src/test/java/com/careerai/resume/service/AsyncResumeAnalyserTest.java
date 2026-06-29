package com.careerai.resume.service;

import com.careerai.resume.config.KafkaConfig;
import com.careerai.resume.domain.entity.Resume;
import com.careerai.resume.domain.entity.ResumeAnalysis;
import com.careerai.resume.domain.enums.ResumeStatus;
import com.careerai.resume.dto.ai.ResumeAnalysisResult;
import com.careerai.resume.dto.response.ResumeAnalysisResponse;
import com.careerai.resume.event.ResumeAnalysedEvent;
import com.careerai.resume.mapper.ResumeMapper;
import com.careerai.resume.repository.ResumeAnalysisRepository;
import com.careerai.resume.repository.ResumeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AsyncResumeAnalyserTest {

    @Mock private ResumeRepository resumeRepository;
    @Mock private ResumeAnalysisRepository analysisRepository;
    @Mock private AiAnalysisService aiAnalysisService;
    @Mock private ResumeMapper resumeMapper;
    @Mock private RedisTemplate<String, ResumeAnalysisResponse> analysisRedisTemplate;
    @Mock private ValueOperations<String, ResumeAnalysisResponse> valueOperations;
    @Mock private KafkaTemplate<UUID, ResumeAnalysedEvent> kafkaTemplate;

    private AsyncResumeAnalyser analyser;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(analysisRedisTemplate.opsForValue()).thenReturn(valueOperations);
        analyser = new AsyncResumeAnalyser(resumeRepository, analysisRepository, aiAnalysisService,
                resumeMapper, analysisRedisTemplate, kafkaTemplate, new ObjectMapper());
    }

    @Test
    void analyse_success_persistsCachesAndPublishesEvent() {
        UUID resumeId = UUID.randomUUID();
        Resume resume = Resume.builder()
                .id(resumeId).userId(USER_ID).userEmail("jane@example.com")
                .extractedText("text").status(ResumeStatus.PROCESSING).build();
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(resume));
        when(aiAnalysisService.analyseResume("text", "Backend Engineer")).thenReturn(sampleResult());
        when(resumeMapper.toAnalysisResponse(any(ResumeAnalysis.class))).thenReturn(null);

        analyser.analyse(resumeId, "Backend Engineer").join();

        assertThat(resume.getStatus()).isEqualTo(ResumeStatus.ANALYSED);
        assertThat(resume.getAnalysedAt()).isNotNull();
        verify(analysisRepository).save(any(ResumeAnalysis.class));

        ArgumentCaptor<ResumeAnalysedEvent> event = ArgumentCaptor.forClass(ResumeAnalysedEvent.class);
        verify(kafkaTemplate).send(eq(KafkaConfig.RESUME_ANALYSED_TOPIC), eq(resumeId), event.capture());
        assertThat(event.getValue().atsScore()).isEqualTo(85);
        assertThat(event.getValue().extractedSkills()).containsExactly("Java");
    }

    @Test
    void analyse_aiFailure_marksResumeFailedAndSkipsPublish() {
        UUID resumeId = UUID.randomUUID();
        Resume resume = Resume.builder().id(resumeId).userId(USER_ID).extractedText("text")
                .status(ResumeStatus.PROCESSING).build();
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(resume));
        when(aiAnalysisService.analyseResume(any(), any())).thenThrow(new RuntimeException("api down"));

        analyser.analyse(resumeId, null).join();

        assertThat(resume.getStatus()).isEqualTo(ResumeStatus.FAILED);
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    private static ResumeAnalysisResult sampleResult() {
        return new ResumeAnalysisResult(85, "Solid candidate", 5, "BACHELOR",
                List.of("Backend Engineer"), List.of("Java"), List.of("No metrics"),
                List.of("Add metrics"), List.of("Spring"), List.of("Kafka"),
                List.of(new ResumeAnalysisResult.ExtractedSkill("Java", "TECHNICAL", "EXPERT", 5, false)));
    }
}
