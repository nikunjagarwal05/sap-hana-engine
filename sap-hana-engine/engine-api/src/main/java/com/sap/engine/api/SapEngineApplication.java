package com.sap.engine.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.sap.engine")
@EntityScan(basePackages = "com.sap.engine.persistence.entity")
@EnableJpaRepositories(basePackages = "com.sap.engine.persistence.repository")
@EnableScheduling
public class SapEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(SapEngineApplication.class, args);
    }
}
