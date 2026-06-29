package com.careerai.resume.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * AWS S3 clients. When {@code app.aws.s3.endpoint} is set (LocalStack in dev) the endpoint is
 * overridden and path-style access enabled; in prod it is left blank to hit real S3.
 */
@Configuration
public class S3Config {

    @Value("${app.aws.region:us-east-1}")
    private String region;

    @Value("${app.aws.access-key:test}")
    private String accessKey;

    @Value("${app.aws.secret-key:test}")
    private String secretKey;

    @Value("${app.aws.s3.endpoint:}")
    private String endpoint;

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    @Bean
    public S3AsyncClient s3AsyncClient() {
        S3AsyncClientBuilder builder = S3AsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());
        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint))
                    .forcePathStyle(true);
        }
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());
        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
        return builder.build();
    }
}
