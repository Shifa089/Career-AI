package com.careerai.interview.config;

import com.careerai.interview.event.InterviewCompletedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.UUIDSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka producer wiring for the {@code interview.completed} topic: UUID keys, JSON-serialized
 * {@link InterviewCompletedEvent} values.
 */
@Configuration
public class KafkaConfig {

    public static final String INTERVIEW_COMPLETED_TOPIC = "interview.completed";

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public NewTopic interviewCompletedTopic() {
        return TopicBuilder.name(INTERVIEW_COMPLETED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public ProducerFactory<UUID, InterviewCompletedEvent> interviewProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, UUIDSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<UUID, InterviewCompletedEvent> interviewKafkaTemplate() {
        return new KafkaTemplate<>(interviewProducerFactory());
    }
}
