package com.careerai.jobmatch.service;

import com.careerai.jobmatch.domain.entity.ResumeEmbedding;
import com.careerai.jobmatch.exception.EmbeddingGenerationException;
import com.careerai.jobmatch.repository.ResumeEmbeddingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringAiEmbeddingServiceImplTest {

    @Mock private EmbeddingModel embeddingModel;
    @Mock private ResumeEmbeddingRepository resumeEmbeddingRepository;

    private SpringAiEmbeddingServiceImpl service() {
        return new SpringAiEmbeddingServiceImpl(embeddingModel, resumeEmbeddingRepository, new ObjectMapper());
    }

    @Test
    void generateEmbedding_returnsModelVector() {
        when(embeddingModel.embed("hello")).thenReturn(new float[]{0.1f, 0.2f});

        assertThat(service().generateEmbedding("hello")).containsExactly(0.1f, 0.2f);
    }

    @Test
    void generateEmbedding_truncatesLongText() {
        String longText = "a".repeat(9000);
        when(embeddingModel.embed(anyString())).thenAnswer(inv -> {
            String arg = inv.getArgument(0);
            assertThat(arg).hasSize(8000);
            return new float[]{1f};
        });

        assertThat(service().generateEmbedding(longText)).containsExactly(1f);
    }

    @Test
    void generateEmbedding_blankText_throws() {
        assertThatThrownBy(() -> service().generateEmbedding("  "))
                .isInstanceOf(EmbeddingGenerationException.class);
    }

    @Test
    void generateEmbedding_modelFailure_wrapped() {
        when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("api down"));

        assertThatThrownBy(() -> service().generateEmbedding("text"))
                .isInstanceOf(EmbeddingGenerationException.class);
    }

    @Test
    void generateAndSaveResumeEmbedding_createsWhenAbsent() {
        UUID resumeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.3f});
        when(resumeEmbeddingRepository.findByResumeId(resumeId)).thenReturn(Optional.empty());
        when(resumeEmbeddingRepository.save(any(ResumeEmbedding.class))).thenAnswer(inv -> inv.getArgument(0));

        ResumeEmbedding saved = service().generateAndSaveResumeEmbedding(
                resumeId, userId, "resume text", List.of("Java", "Kafka"));

        assertThat(saved.getResumeId()).isEqualTo(resumeId);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getExtractedSkills()).contains("Java");
        assertThat(saved.getEmbedding()).containsExactly(0.3f);
    }

    @Test
    void generateAndSaveResumeEmbedding_updatesExisting() {
        UUID resumeId = UUID.randomUUID();
        ResumeEmbedding existing = ResumeEmbedding.builder().id(UUID.randomUUID()).resumeId(resumeId).build();
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.9f});
        when(resumeEmbeddingRepository.findByResumeId(resumeId)).thenReturn(Optional.of(existing));
        when(resumeEmbeddingRepository.save(any(ResumeEmbedding.class))).thenAnswer(inv -> inv.getArgument(0));

        ResumeEmbedding saved = service().generateAndSaveResumeEmbedding(
                resumeId, UUID.randomUUID(), "text", null);

        assertThat(saved.getId()).isEqualTo(existing.getId());
        assertThat(saved.getEmbedding()).containsExactly(0.9f);
    }

    @Test
    void getResumeEmbedding_delegatesToRepository() {
        UUID resumeId = UUID.randomUUID();
        ResumeEmbedding embedding = ResumeEmbedding.builder().resumeId(resumeId).build();
        when(resumeEmbeddingRepository.findByResumeId(resumeId)).thenReturn(Optional.of(embedding));

        assertThat(service().getResumeEmbedding(resumeId)).contains(embedding);
    }
}
