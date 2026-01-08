package com.enterprise.reportgenerator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class ReportGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportGeneratorApplication.class, args);
    }

    @Bean
    public CommandLineRunner init(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM USERS", Integer.class);
                log.info("[STARTUP] Database connected successfully. Found {} users in USERS table.", count);
            } catch (Exception e) {
                log.error("[STARTUP] Database check failed! Error: {}", e.getMessage());
                log.warn("[STARTUP] If the USERS table is missing, please run the SQL migration script provided.");
            }
        };
    }
}
