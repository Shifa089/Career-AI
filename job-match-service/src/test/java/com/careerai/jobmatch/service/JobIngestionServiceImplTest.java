package com.careerai.jobmatch.service;

import com.careerai.jobmatch.domain.entity.JobListing;
import com.careerai.jobmatch.repository.JobEmbeddingRepository;
import com.careerai.jobmatch.repository.JobListingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobIngestionServiceImplTest {

    @Mock private JobListingRepository jobListingRepository;
    @Mock private JobEmbeddingRepository jobEmbeddingRepository;
    @Mock private EmbeddingService embeddingService;

    private JobIngestionServiceImpl newService(WebClient webClient, String appId, String appKey) {
        return new JobIngestionServiceImpl(webClient, jobListingRepository, jobEmbeddingRepository,
                embeddingService, new ObjectMapper(), "gb", appId, appKey, true);
    }

    @Test
    void ingestSampleJobs_embedsOnlyListingsWithoutEmbedding() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        JobListing j1 = JobListing.builder().id(id1).title("A").company("c").descriptionText("d").build();
        JobListing j2 = JobListing.builder().id(id2).title("B").company("c").descriptionText("d").build();
        when(jobListingRepository.findByActiveTrue()).thenReturn(List.of(j1, j2));
        when(jobEmbeddingRepository.existsByJobListingId(id1)).thenReturn(true);
        when(jobEmbeddingRepository.existsByJobListingId(id2)).thenReturn(false);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{0.1f});

        int embedded = newService(mock(WebClient.class), "", "").ingestSampleJobs();

        assertThat(embedded).isEqualTo(1);
        verify(jobEmbeddingRepository).save(any());
    }

    @Test
    void onStartup_backfillsSampleEmbeddings() {
        when(jobListingRepository.findByActiveTrue()).thenReturn(List.of());

        newService(mock(WebClient.class), "", "").onStartup();

        verify(jobListingRepository).findByActiveTrue();
    }

    @Test
    void ingestFromAdzuna_withoutCredentials_returnsZero() {
        int ingested = newService(mock(WebClient.class), "", "").ingestFromAdzuna("java", "london", 10);

        assertThat(ingested).isZero();
        verify(jobListingRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void ingestFromAdzuna_mapsAndPersistsNewJobs() {
        WebClient webClient = mock(WebClient.class, RETURNS_DEEP_STUBS);
        JobIngestionServiceImpl.AdzunaJob job = new JobIngestionServiceImpl.AdzunaJob(
                "123", "Remote Java Engineer", "Build remote services",
                new JobIngestionServiceImpl.AdzunaCompany("Acme"),
                new JobIngestionServiceImpl.AdzunaLocation("London"),
                "http://job", 50000.0, 70000.0);
        JobIngestionServiceImpl.AdzunaResponse response = new JobIngestionServiceImpl.AdzunaResponse(List.of(job));

        when(webClient.get()
                .uri(any(Function.class))
                .retrieve()
                .bodyToMono(JobIngestionServiceImpl.AdzunaResponse.class)
                .block(any(Duration.class))).thenReturn(response);
        when(jobListingRepository.existsByExternalId("adzuna-123")).thenReturn(false);
        when(jobListingRepository.save(any(JobListing.class))).thenAnswer(inv -> {
            JobListing l = inv.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{0.2f});

        int ingested = newService(webClient, "app", "key").ingestFromAdzuna("java", "london", 10);

        assertThat(ingested).isEqualTo(1);
        verify(jobEmbeddingRepository).save(any());
    }
}
