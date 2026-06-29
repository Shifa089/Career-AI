package com.careerai.jobmatch;

import com.careerai.common.exception.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableDiscoveryClient
@EnableScheduling
@SpringBootApplication
@Import(GlobalExceptionHandler.class) // common-lib handler is not component-scanned from this package
public class JobMatchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobMatchServiceApplication.class, args);
    }
}
