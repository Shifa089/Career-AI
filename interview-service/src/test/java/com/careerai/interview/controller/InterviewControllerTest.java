package com.careerai.interview.controller;

import com.careerai.interview.domain.enums.InterviewType;
import com.careerai.interview.dto.request.CreateSessionRequest;
import com.careerai.interview.dto.response.InterviewStatsResponse;
import com.careerai.interview.dto.response.SessionResponse;
import com.careerai.interview.security.AuthenticatedUser;
import com.careerai.interview.service.InterviewSessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewControllerTest {

    @Mock private InterviewSessionService interviewSessionService;
    @InjectMocks private InterviewController controller;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "jane@example.com";

    @BeforeEach
    void authenticate() {
        AuthenticatedUser user = new AuthenticatedUser(USER_ID, EMAIL);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_returns201() {
        CreateSessionRequest request = new CreateSessionRequest(
                "Backend Engineer", null, null, InterviewType.TECHNICAL, 5);
        SessionResponse response = sample();
        when(interviewSessionService.createSession(request, USER_ID, EMAIL)).thenReturn(response);

        ResponseEntity<?> result = controller.create(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(interviewSessionService).createSession(request, USER_ID, EMAIL);
    }

    @Test
    void list_delegatesWithPageable() {
        Page<SessionResponse> page = new PageImpl<>(List.of(sample()));
        when(interviewSessionService.getUserSessions(eq(USER_ID), any(Pageable.class))).thenReturn(page);

        ResponseEntity<?> result = controller.list(Pageable.unpaged());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void get_and_feedback_and_abandon_and_stats() {
        UUID sessionId = UUID.randomUUID();
        when(interviewSessionService.getSessionDetails(sessionId, USER_ID)).thenReturn(sample());
        when(interviewSessionService.getUserStats(USER_ID))
                .thenReturn(new InterviewStatsResponse(3, 2, 0.66, 75.0));

        assertThat(controller.get(sessionId).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.stats().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.abandon(sessionId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(interviewSessionService).abandonSession(sessionId, USER_ID);

        when(interviewSessionService.getFeedback(sessionId, USER_ID)).thenReturn(null);
        assertThat(controller.feedback(sessionId).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private SessionResponse sample() {
        return new SessionResponse(UUID.randomUUID(), "Backend Engineer", null,
                InterviewType.TECHNICAL, null, 5, 0, null, null, null, null);
    }
}
