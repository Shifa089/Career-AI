package com.careerai.jobmatch.service;

import com.careerai.jobmatch.dto.ai.SkillGapResult;
import com.careerai.jobmatch.exception.AiAnalysisException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClaudeSkillGapServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ClaudeSkillGapServiceImpl serviceReturning(String content) {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn(content);
        return new ClaudeSkillGapServiceImpl(builder, objectMapper);
    }

    @Test
    void analyseSkillGap_parsesJsonResponse() {
        String json = """
                {"matchedSkills":["Java"],"missingSkills":["Kafka"],
                "partialMatches":[{"skill":"Spring","candidateLevel":"basic","requiredLevel":"advanced"}],
                "gapScore":70,"readinessLevel":"NEEDS_WORK",
                "learningPath":[{"skill":"Kafka","priority":"HIGH","estimatedWeeks":4,
                "resources":[{"title":"Kafka 101","url":"http://x","type":"COURSE"}]}],
                "summary":"Close some gaps"}
                """;
        ClaudeSkillGapServiceImpl service = serviceReturning(json);

        SkillGapResult result = service.analyseSkillGap(List.of("Java"), List.of("Java", "Kafka"), "Backend Engineer");

        assertThat(result.matchedSkills()).containsExactly("Java");
        assertThat(result.missingSkills()).containsExactly("Kafka");
        assertThat(result.gapScore()).isEqualTo(70);
        assertThat(result.readinessLevel()).isEqualTo("NEEDS_WORK");
        assertThat(result.partialMatches()).hasSize(1);
        assertThat(result.learningPath().get(0).resources().get(0).type()).isEqualTo("COURSE");
    }

    @Test
    void analyseSkillGap_stripsMarkdownFences() {
        String fenced = "```json\n{\"matchedSkills\":[],\"missingSkills\":[],\"gapScore\":0,"
                + "\"readinessLevel\":\"READY\",\"summary\":\"ok\"}\n```";
        ClaudeSkillGapServiceImpl service = serviceReturning(fenced);

        SkillGapResult result = service.analyseSkillGap(List.of(), List.of(), "Role");

        assertThat(result.readinessLevel()).isEqualTo("READY");
    }

    @Test
    void analyseSkillGap_malformedJson_throws() {
        ClaudeSkillGapServiceImpl service = serviceReturning("not json at all");

        assertThatThrownBy(() -> service.analyseSkillGap(List.of(), List.of(), "Role"))
                .isInstanceOf(AiAnalysisException.class);
    }

    @Test
    void analyseSkillGap_emptyResponse_throws() {
        ClaudeSkillGapServiceImpl service = serviceReturning("   ");

        assertThatThrownBy(() -> service.analyseSkillGap(List.of(), List.of(), "Role"))
                .isInstanceOf(AiAnalysisException.class);
    }
}
