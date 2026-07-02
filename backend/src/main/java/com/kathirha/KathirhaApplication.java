package com.kathirha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Kathirha (كثّرها) — gamified, AI-powered savings super-app.
 * Entry point. Scheduling is enabled for payday/daily nudges.
 */
@SpringBootApplication
@EnableScheduling
public class KathirhaApplication {
    public static void main(String[] args) {
        SpringApplication.run(KathirhaApplication.class, args);
    }
}
