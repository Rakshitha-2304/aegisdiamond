package com.aegisdiamond.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = {net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration.class})
@ComponentScan(basePackages = {"com.aegisdiamond"})
@EnableJpaRepositories(basePackages = {"com.aegisdiamond"})
@EntityScan(basePackages = {"com.aegisdiamond"})
public class AegisDiamondApplication {
    private static final Logger logger = LoggerFactory.getLogger(AegisDiamondApplication.class);

    public static void main(String[] args) {
        logger.info("Starting Aegis Diamond High-Security Logistic System...");
        SpringApplication.run(AegisDiamondApplication.class, args);
        logger.info("Aegis Diamond System is up and running.");
    }
}
