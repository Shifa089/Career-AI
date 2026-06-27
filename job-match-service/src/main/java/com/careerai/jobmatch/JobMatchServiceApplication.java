package com.careerai.jobmatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class JobMatchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobMatchServiceApplication.class, args);
    }
}
