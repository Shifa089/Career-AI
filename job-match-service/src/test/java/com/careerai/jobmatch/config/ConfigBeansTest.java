package com.careerai.jobmatch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Lightweight construction tests for the {@code @Configuration} beans that don't require a running
 * Spring context (no external connections are opened at construction time).
 */
class ConfigBeansTest {

    @Test
    void redisConfig_buildsCacheManager() {
        RedisCacheManager manager = new RedisConfig()
                .cacheManager(mock(RedisConnectionFactory.class), new ObjectMapper(), 60);
        assertThat(manager).isNotNull();
    }

    @Test
    void webClientConfig_buildsAdzunaClient() {
        WebClient client = new WebClientConfig().adzunaWebClient("https://api.adzuna.com/v1/api");
        assertThat(client).isNotNull();
    }

    @Test
    void openApiConfig_hasTitle() {
        var openApi = new OpenApiConfig().jobMatchServiceOpenAPI();
        assertThat(openApi.getInfo().getTitle()).isEqualTo("CareerAI Job Match Service API");
    }

    @Test
    void kafkaConfig_declaresResumeAnalysedTopic() {
        var topic = new KafkaConfig().resumeAnalysedTopic();
        assertThat(topic.name()).isEqualTo("resume.analysed");
        assertThat(topic.numPartitions()).isEqualTo(3);
    }
}
