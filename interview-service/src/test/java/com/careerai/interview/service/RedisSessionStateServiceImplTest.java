package com.careerai.interview.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisSessionStateServiceImplTest {

    @Mock private RedisTemplate<String, ActiveSessionState> redisTemplate;
    @Mock private ValueOperations<String, ActiveSessionState> valueOps;

    private RedisSessionStateServiceImpl service;

    private static final UUID SESSION_ID = UUID.randomUUID();

    private void initService() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new RedisSessionStateServiceImpl(redisTemplate);
    }

    @Test
    void saveAndGet_useExpectedKeyAndTtl() {
        initService();
        ActiveSessionState state = new ActiveSessionState(
                SESSION_ID, UUID.randomUUID(), 1, List.of("Q1"), LocalDateTime.now());

        service.saveActiveSession(SESSION_ID, state);
        verify(valueOps).set(eq("interview:active:" + SESSION_ID), eq(state), eq(Duration.ofHours(4)));

        when(valueOps.get("interview:active:" + SESSION_ID)).thenReturn(state);
        assertThat(service.getActiveSession(SESSION_ID)).contains(state);
    }

    @Test
    void appendQuestion_incrementsNumberAndAppendsText() {
        initService();
        ActiveSessionState existing = new ActiveSessionState(
                SESSION_ID, UUID.randomUUID(), 1, List.of("Q1"), LocalDateTime.now());
        when(valueOps.get("interview:active:" + SESSION_ID)).thenReturn(existing);

        service.appendQuestion(SESSION_ID, 2, "Q2");

        ArgumentCaptor<ActiveSessionState> captor = ArgumentCaptor.forClass(ActiveSessionState.class);
        verify(valueOps).set(eq("interview:active:" + SESSION_ID), captor.capture(), eq(Duration.ofHours(4)));
        assertThat(captor.getValue().currentQuestionNumber()).isEqualTo(2);
        assertThat(captor.getValue().previousQuestions()).containsExactly("Q1", "Q2");
    }

    @Test
    void appendQuestion_missingState_isNoOp() {
        initService();
        when(valueOps.get("interview:active:" + SESSION_ID)).thenReturn(null);

        service.appendQuestion(SESSION_ID, 2, "Q2");

        verify(valueOps, org.mockito.Mockito.never())
                .set(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(Duration.class));
    }

    @Test
    void getActiveSession_absent_returnsEmpty() {
        initService();
        when(valueOps.get("interview:active:" + SESSION_ID)).thenReturn(null);
        assertThat(service.getActiveSession(SESSION_ID)).isEmpty();
    }

    @Test
    void removeActiveSession_deletesKey() {
        service = new RedisSessionStateServiceImpl(redisTemplate);
        service.removeActiveSession(SESSION_ID);
        verify(redisTemplate).delete("interview:active:" + SESSION_ID);
    }
}
