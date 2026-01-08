package com.enterprise.reportgenerator.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class FlywayRepairConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            try {
                log.info("[FLYWAY-REPAIR] Attempting to repair failed migrations...");
                flyway.repair();
                log.info("[FLYWAY-REPAIR] Repair completed successfully.");
            } catch (Exception e) {
                log.warn("[FLYWAY-REPAIR] Repair failed or not needed: {}", e.getMessage());
            }

            log.info("[FLYWAY-REPAIR] Starting migration...");
            flyway.migrate();
            log.info("[FLYWAY-REPAIR] Migration completed.");
        };
    }
}
