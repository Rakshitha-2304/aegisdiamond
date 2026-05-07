package com.aegisdiamond.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.aegisdiamond"})
@EnableJpaRepositories(basePackages = {"com.aegisdiamond"})
@EntityScan(basePackages = {"com.aegisdiamond"})
public class AegisDiamondApplication {
    public static void main(String[] args) {
        SpringApplication.run(AegisDiamondApplication.class, args);
    }
}
