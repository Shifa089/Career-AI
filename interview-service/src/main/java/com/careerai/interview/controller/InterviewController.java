package com.careerai.interview.controller;

import com.careerai.common.dto.ApiResponse;
import com.careerai.interview.dto.request.CreateSessionRequest;
import com.careerai.interview.dto.response.FeedbackResponse;
import com.careerai.interview.dto.response.InterviewStatsResponse;
import com.careerai.interview.dto.response.SessionResponse;
import com.careerai.interview.security.AuthenticatedUser;
import com.careerai.interview.service.InterviewSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST endpoints for interview session lifecycle and retrieval. The real-time question/answer loop
 * happens over WebSocket (see {@code InterviewWebSocketController}); these endpoints create, list,
 * inspect and abandon sessions. The caller's identity comes from the gateway-forwarded
 * {@code X-User-*} headers.
 */
@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
@Tag(name = "Interviews", description = "Create, manage and review mock interview sessions")
public class InterviewController {

    private final InterviewSessionService interviewSessionService;

    @Operation(summary = "Create a new interview session")
    @PostMapping
    public ResponseEntity<ApiResponse<SessionResponse>> create(@Valid @RequestBody CreateSessionRequest request) {
        AuthenticatedUser user = AuthenticatedUser.current();
        SessionResponse response = interviewSessionService.createSession(request, user.userId(), user.email());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Interview session created", response));
    }

    @Operation(summary = "List the current user's interview sessions")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<SessionResponse>>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        AuthenticatedUser user = AuthenticatedUser.current();
        return ResponseEntity.ok(ApiResponse.success(
                interviewSessionService.getUserSessions(user.userId(), pageable)));
    }

    @Operation(summary = "Get a single interview session")
    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<SessionResponse>> get(@PathVariable UUID sessionId) {
        AuthenticatedUser user = AuthenticatedUser.current();
        return ResponseEntity.ok(ApiResponse.success(
                interviewSessionService.getSessionDetails(sessionId, user.userId())));
    }

    @Operation(summary = "Get the final feedback for a completed session")
    @GetMapping("/{sessionId}/feedback")
    public ResponseEntity<ApiResponse<FeedbackResponse>> feedback(@PathVariable UUID sessionId) {
        AuthenticatedUser user = AuthenticatedUser.current();
        return ResponseEntity.ok(ApiResponse.success(
                interviewSessionService.getFeedback(sessionId, user.userId())));
    }

    @Operation(summary = "Abandon an interview session")
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> abandon(@PathVariable UUID sessionId) {
        AuthenticatedUser user = AuthenticatedUser.current();
        interviewSessionService.abandonSession(sessionId, user.userId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get the current user's interview statistics")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<InterviewStatsResponse>> stats() {
        AuthenticatedUser user = AuthenticatedUser.current();
        return ResponseEntity.ok(ApiResponse.success(interviewSessionService.getUserStats(user.userId())));
    }
}
