package com.gupta.linkly.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DatabaseCleanupConfig {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void cleanupLegacyTables() {
        try {
            log.info("Checking and cleaning up legacy database tables...");
            jdbcTemplate.execute("DROP TABLE IF EXISTS password_reset_tokens CASCADE");
            log.info("Legacy tables cleaned successfully.");
        } catch (Exception e) {
            log.warn("Failed to drop legacy tables, might not exist or lack permissions: {}", e.getMessage());
        }
    }
}
