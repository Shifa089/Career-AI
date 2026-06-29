package com.careerai.jobmatch.controller;

import com.careerai.common.dto.ApiResponse;
import com.careerai.jobmatch.domain.enums.MatchStatus;
import com.careerai.jobmatch.dto.request.FindMatchesRequest;
import com.careerai.jobmatch.dto.response.JobListingResponse;
import com.careerai.jobmatch.dto.response.JobMatchResponse;
import com.careerai.jobmatch.dto.response.LearningPathResponse;
import com.careerai.jobmatch.dto.response.SkillGapResponse;
import com.careerai.jobmatch.security.AuthenticatedUser;
import com.careerai.jobmatch.service.JobIngestionService;
import com.careerai.jobmatch.service.JobMatchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobMatchControllerTest {

    @Mock private JobMatchService jobMatchService;
    @Mock private JobIngestionService jobIngestionService;

    private JobMatchController controller;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new JobMatchController(jobMatchService, jobIngestionService);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(USER_ID, "jane@example.com"), null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findMatches_delegatesToService() {
        UUID resumeId = UUID.randomUUID();
        when(jobMatchService.findMatchesForResume(resumeId, USER_ID, 10)).thenReturn(List.of(match()));

        var response = controller.findMatches(new FindMatchesRequest(resumeId, null, null));

        assertThat(body(response).data()).hasSize(1);
    }

    @Test
    void getMyMatches_passesStatusFilter() {
        Page<JobMatchResponse> page = new PageImpl<>(List.of(match()));
        when(jobMatchService.getMyMatches(USER_ID, MatchStatus.SAVED, PageRequest.of(0, 20))).thenReturn(page);

        var response = controller.getMyMatches(MatchStatus.SAVED, PageRequest.of(0, 20));

        assertThat(body(response).data().getTotalElements()).isEqualTo(1);
    }

    @Test
    void getMatch_returnsMatch() {
        UUID matchId = UUID.randomUUID();
        when(jobMatchService.getMatchById(matchId, USER_ID)).thenReturn(match());

        assertThat(body(controller.getMatch(matchId)).data()).isNotNull();
    }

    @Test
    void getSkillGap_returnsAnalysis() {
        UUID matchId = UUID.randomUUID();
        SkillGapResponse gap = new SkillGapResponse("Backend", List.of("Java"), List.of("Kafka"),
                List.of(), 60, "NEEDS_WORK", "summary");
        when(jobMatchService.analyseSkillGap(matchId, USER_ID)).thenReturn(gap);

        assertThat(body(controller.getSkillGap(matchId)).data().gapScore()).isEqualTo(60);
    }

    @Test
    void getLearningPath_returnsPath() {
        UUID matchId = UUID.randomUUID();
        when(jobMatchService.getLearningPath(matchId, USER_ID))
                .thenReturn(new LearningPathResponse("Backend", 8, List.of()));

        assertThat(body(controller.getLearningPath(matchId)).data().totalEstimatedWeeks()).isEqualTo(8);
    }

    @Test
    void updateStatus_returnsUpdated() {
        UUID matchId = UUID.randomUUID();
        when(jobMatchService.updateMatchStatus(matchId, MatchStatus.APPLIED, USER_ID)).thenReturn(match());

        assertThat(body(controller.updateStatus(matchId, MatchStatus.APPLIED)).data()).isNotNull();
    }

    @Test
    void searchJobs_returnsPage() {
        when(jobMatchService.searchJobs("java", "remote", PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(listing())));

        assertThat(body(controller.searchJobs("java", "remote", PageRequest.of(0, 20))).data().getTotalElements())
                .isEqualTo(1);
    }

    @Test
    void ingest_returnsCount() {
        when(jobIngestionService.ingestFromAdzuna("java", "london", 25)).thenReturn(7);

        ApiResponse<Map<String, Integer>> body = body(controller.ingest("java", "london", 25));

        assertThat(body.data().get("ingested")).isEqualTo(7);
    }

    private static <T> ApiResponse<T> body(org.springframework.http.ResponseEntity<ApiResponse<T>> response) {
        return response.getBody();
    }

    private JobMatchResponse match() {
        return new JobMatchResponse(UUID.randomUUID(), listing(), 0.9, 90, List.of("Java"), List.of(),
                "PENDING_REVIEW", null);
    }

    private JobListingResponse listing() {
        return new JobListingResponse(UUID.randomUUID(), "Backend", "Acme", "Remote", "REMOTE", "desc",
                List.of("Java"), List.of(), "$100k", "SENIOR", "http://x", null);
    }
}
