package com.careerai.interview;

import com.careerai.common.exception.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

/**
 * Interview-service entry point. Imports the shared {@link GlobalExceptionHandler} (which lives in
 * common-lib, outside this service's component-scan root) so common-lib exceptions are rendered as
 * standard {@code ApiResponse} errors.
 */
@EnableDiscoveryClient
@SpringBootApplication
@Import(GlobalExceptionHandler.class)
public class InterviewServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewServiceApplication.class, args);
    }
}
