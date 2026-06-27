package com.careerai.resume;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class ResumeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumeServiceApplication.class, args);
    }
}
