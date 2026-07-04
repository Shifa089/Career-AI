package com.careerai.jobmatch.event.consumer;

import com.careerai.jobmatch.event.ResumeAnalysedEvent;
import com.careerai.jobmatch.service.EmbeddingService;
import com.careerai.jobmatch.service.JobMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Consumes {@code resume.analysed} events produced by resume-service. For each event it builds a
 * resume embedding from the extracted skills/roles and computes the user's job matches (which are
 * cached for the REST API). Failures are retried up to 3 times, then routed to a dead-letter topic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResumeAnalysedEventConsumer {

    private static final int DEFAULT_MATCH_LIMIT = 10;

    private final EmbeddingService embeddingService;
    private final JobMatchService jobMatchService;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    @KafkaListener(topics = "resume.analysed", groupId = "job-match-service")
    public void onResumeAnalysed(ResumeAnalysedEvent event) {
        log.info("Received resume.analysed for resume {} (user {})", event.resumeId(), event.userId());

        String embeddingText = buildEmbeddingText(event);
        embeddingService.generateAndSaveResumeEmbedding(
                event.resumeId(), event.userId(), embeddingText, event.extractedSkills());

        List<?> matches = jobMatchService.findMatchesForResume(
                event.resumeId(), event.userId(), DEFAULT_MATCH_LIMIT);

        log.info("Computed {} matches for resume {} from resume.analysed event", matches.size(), event.resumeId());
    }

    @DltHandler
    public void onDeadLetter(ResumeAnalysedEvent event) {
        log.error("resume.analysed event for resume {} exhausted retries and was sent to the DLT",
                event == null ? "<null>" : event.resumeId());
    }

    private String buildEmbeddingText(ResumeAnalysedEvent event) {
        StringBuilder sb = new StringBuilder();
        if (event.extractedSkills() != null && !event.extractedSkills().isEmpty()) {
            sb.append("Skills: ").append(String.join(", ", event.extractedSkills())).append(". ");
        }
        if (event.targetRoles() != null && !event.targetRoles().isEmpty()) {
            sb.append("Target roles: ").append(String.join(", ", event.targetRoles())).append(".");
        }
        return sb.toString();
    }
}
