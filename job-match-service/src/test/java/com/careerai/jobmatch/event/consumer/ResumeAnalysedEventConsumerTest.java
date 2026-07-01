package com.careerai.jobmatch.event.consumer;

import com.careerai.jobmatch.event.ResumeAnalysedEvent;
import com.careerai.jobmatch.service.EmbeddingService;
import com.careerai.jobmatch.service.JobMatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeAnalysedEventConsumerTest {

    @Mock private EmbeddingService embeddingService;
    @Mock private JobMatchService jobMatchService;

    @Test
    void onResumeAnalysed_embedsThenMatches() {
        ResumeAnalysedEventConsumer consumer = new ResumeAnalysedEventConsumer(embeddingService, jobMatchService);
        UUID resumeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ResumeAnalysedEvent event = new ResumeAnalysedEvent(resumeId, userId, "jane@example.com",
                List.of("Java", "Spring"), List.of("Backend Engineer"), 90, LocalDateTime.now());
        when(jobMatchService.findMatchesForResume(eq(resumeId), eq(userId), anyInt())).thenReturn(List.of());

        consumer.onResumeAnalysed(event);

        verify(embeddingService).generateAndSaveResumeEmbedding(eq(resumeId), eq(userId),
                contains("Java"), eq(List.of("Java", "Spring")));
        verify(jobMatchService).findMatchesForResume(resumeId, userId, 10);
    }

    @Test
    void onResumeAnalysed_handlesEmptySkillsAndRoles() {
        ResumeAnalysedEventConsumer consumer = new ResumeAnalysedEventConsumer(embeddingService, jobMatchService);
        UUID resumeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ResumeAnalysedEvent event = new ResumeAnalysedEvent(resumeId, userId, "j@x.com",
                List.of(), null, null, LocalDateTime.now());
        when(jobMatchService.findMatchesForResume(any(), any(), anyInt())).thenReturn(List.of());

        consumer.onResumeAnalysed(event);

        verify(embeddingService).generateAndSaveResumeEmbedding(eq(resumeId), eq(userId), eq(""), eq(List.of()));
    }

    @Test
    void onDeadLetter_doesNotThrow() {
        ResumeAnalysedEventConsumer consumer = new ResumeAnalysedEventConsumer(embeddingService, jobMatchService);
        ResumeAnalysedEvent event = new ResumeAnalysedEvent(UUID.randomUUID(), UUID.randomUUID(), "j@x.com",
                List.of(), List.of(), 0, LocalDateTime.now());

        assertThatCode(() -> consumer.onDeadLetter(event)).doesNotThrowAnyException();
        assertThatCode(() -> consumer.onDeadLetter(null)).doesNotThrowAnyException();
    }
}
