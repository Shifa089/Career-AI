package com.careerai.resume.config;

import com.careerai.resume.event.ResumeAnalysedEvent;
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
 * Kafka producer wiring for the {@code resume.analysed} topic: UUID keys, JSON-serialized
 * {@link ResumeAnalysedEvent} values.
 */
@Configuration
public class KafkaConfig {

    public static final String RESUME_ANALYSED_TOPIC = "resume.analysed";

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public org.apache.kafka.clients.admin.NewTopic resumeAnalysedTopic() {
        return TopicBuilder.name(RESUME_ANALYSED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public ProducerFactory<UUID, ResumeAnalysedEvent> resumeProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, UUIDSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<UUID, ResumeAnalysedEvent> resumeKafkaTemplate() {
        return new KafkaTemplate<>(resumeProducerFactory());
    }
}
