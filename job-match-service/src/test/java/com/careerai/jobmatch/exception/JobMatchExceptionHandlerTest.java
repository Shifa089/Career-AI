package com.careerai.jobmatch.exception;

import com.careerai.common.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class JobMatchExceptionHandlerTest {

    private final JobMatchExceptionHandler handler = new JobMatchExceptionHandler();

    @Test
    void embeddingFailure_mapsTo502() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/job-matches/find");

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleUpstreamAiFailure(new EmbeddingGenerationException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().error().code()).isEqualTo("AI_PROVIDER_ERROR");
    }

    @Test
    void aiAnalysisFailure_mapsTo502() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleUpstreamAiFailure(new AiAnalysisException("bad"), new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }
}
