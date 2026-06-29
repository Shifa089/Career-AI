package com.careerai.interview.mapper;

import com.careerai.interview.domain.entity.InterviewFeedback;
import com.careerai.interview.domain.entity.InterviewQuestion;
import com.careerai.interview.domain.entity.InterviewSession;
import com.careerai.interview.domain.enums.QuestionType;
import com.careerai.interview.dto.response.FeedbackResponse;
import com.careerai.interview.dto.response.QuestionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the hand-written JSON-expansion logic in the generated {@link InterviewMapperImpl}.
 */
class InterviewMapperTest {

    private InterviewMapperImpl mapper;

    @BeforeEach
    void setUp() {
        mapper = new InterviewMapperImpl();
        mapper.objectMapper = new ObjectMapper();
    }

    @Test
    void toQuestionResponse_pullsTotalFromSession() {
        InterviewSession session = InterviewSession.builder().totalQuestions(8).build();
        InterviewQuestion question = InterviewQuestion.builder()
                .id(UUID.randomUUID())
                .session(session)
                .questionNumber(3)
                .questionText("Q?")
                .type(QuestionType.CODING)
                .difficulty("HARD")
                .build();

        QuestionResponse response = mapper.toQuestionResponse(question);

        assertThat(response.totalQuestions()).isEqualTo(8);
        assertThat(response.questionNumber()).isEqualTo(3);
        assertThat(response.type()).isEqualTo(QuestionType.CODING);
    }

    @Test
    void toFeedbackResponse_expandsJsonColumns() {
        InterviewSession session = InterviewSession.builder()
                .id(UUID.randomUUID()).overallScore(82).build();
        InterviewFeedback feedback = InterviewFeedback.builder()
                .id(UUID.randomUUID())
                .session(session)
                .technicalScore(80)
                .strongAreas("[\"Algorithms\",\"APIs\"]")
                .improvementAreas("[\"System design\"]")
                .detailedFeedback("Solid.")
                .recommendedResources("[{\"title\":\"DDIA\",\"url\":\"http://x\",\"type\":\"BOOK\"}]")
                .build();

        FeedbackResponse response = mapper.toFeedbackResponse(feedback);

        assertThat(response.sessionId()).isEqualTo(session.getId());
        assertThat(response.overallScore()).isEqualTo(82);
        assertThat(response.strongAreas()).containsExactly("Algorithms", "APIs");
        assertThat(response.improvementAreas()).containsExactly("System design");
        assertThat(response.recommendedResources()).hasSize(1);
        assertThat(response.recommendedResources().get(0).title()).isEqualTo("DDIA");
    }

    @Test
    void toFeedbackResponse_nullJson_yieldsEmptyLists() {
        InterviewFeedback feedback = InterviewFeedback.builder()
                .id(UUID.randomUUID())
                .session(InterviewSession.builder().id(UUID.randomUUID()).build())
                .build();

        FeedbackResponse response = mapper.toFeedbackResponse(feedback);

        assertThat(response.strongAreas()).isEmpty();
        assertThat(response.improvementAreas()).isEmpty();
        assertThat(response.recommendedResources()).isEmpty();
    }
}
