package com.gupta.linkly.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DatabaseSanitizationConfig {
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void sanitizeUsernames() {
        try {
            jdbcTemplate.execute("UPDATE users SET username = TRIM(username) WHERE username LIKE '% ' OR username LIKE ' %'");
            log.info("Sanitized usernames successfully.");
        } catch (Exception e) {
            log.warn("Failed to sanitize usernames: {}", e.getMessage());
        }
    }
}
