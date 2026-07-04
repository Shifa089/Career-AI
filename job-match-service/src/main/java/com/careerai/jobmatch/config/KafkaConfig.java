package com.careerai.jobmatch.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topology for job-match-service. Serializers/deserializers are configured in
 * {@code application.yml}; retry/DLT topics are created automatically by {@code @RetryableTopic}.
 * Declaring the inbound topic here ensures it exists for local runs even before the producer starts.
 */
@Configuration
public class KafkaConfig {

    public static final String RESUME_ANALYSED_TOPIC = "resume.analysed";

    @Bean
    public NewTopic resumeAnalysedTopic() {
        return TopicBuilder.name(RESUME_ANALYSED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
