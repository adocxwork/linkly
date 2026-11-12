package com.gupta.linkly;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full application against a real PostgreSQL, with Redis NOT configured. Proves that:
 * Flyway migrations build a schema Hibernate accepts (ddl-auto=validate), the app starts and
 * works without Redis, and the main user journey works end to end.
 *
 * <p>Database: a throwaway PostgreSQL container (needs Docker, e.g. in GitHub Actions), or set
 * TEST_DB_URL / TEST_DB_USERNAME / TEST_DB_PASSWORD to an EMPTY database. Skipped if neither.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIf("databaseAvailable")
class LinklyApplicationTests {

    private static PostgreSQLContainer postgres;

    static boolean databaseAvailable() {
        return System.getenv("TEST_DB_URL") != null || DockerClientFactory.instance().isDockerAvailable();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String url = System.getenv("TEST_DB_URL");
        if (url != null) {
            registry.add("spring.datasource.url", () -> url);
            registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("TEST_DB_USERNAME", "postgres"));
            registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("TEST_DB_PASSWORD", ""));
        } else {
            postgres = new PostgreSQLContainer("postgres:17-alpine");
            postgres.start();
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("app.redis.url", () -> "");
        registry.add("app.geoip.enabled", () -> "false");
        registry.add("app.admin.password", () -> "admin-password-123");
        registry.add("app.analytics.drain-interval-ms", () -> "200");
        registry.add("jwt.secret", () -> "integration-test-secret-at-least-32-chars");
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper json;

    @Test
    void fullUserJourneyWorksWithoutRedis() throws Exception {
        String token = json(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Ada","username":"ada","email":"ada@example.com","password":"password123","enablePublicMessaging":true}
                        """)).andExpect(status().isCreated())).path("token").asString();

        mvc.perform(post("/api/links").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Docs","originalUrl":"https://example.com/docs","customAlias":"ada-docs"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortUrl").value("ada-docs"));

        mvc.perform(get("/r/ada-docs").header("User-Agent", "Mozilla/5.0 (Windows NT 10.0) Chrome/120.0"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/docs"));

        mvc.perform(get("/api/public/u/ada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links[0].title").value("Docs"))
                .andExpect(jsonPath("$.links[0].clickCount").doesNotExist());

        // the click goes through the in-memory queue (no Redis) and is stored asynchronously
        long clicks = 0;
        for (int i = 0; i < 50 && clicks == 0; i++) {
            Thread.sleep(200);
            clicks = json(mvc.perform(get("/api/links/dashboard").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())).path("totalClicks").asLong();
        }
        assertThat(clicks).isEqualTo(1);
    }

    @Test
    void securityRulesAreEnforced() throws Exception {
        mvc.perform(get("/api/links")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/system/health")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/links").header("Authorization", "Bearer not-a-jwt")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void adminSeesSystemHealth() throws Exception {
        String adminToken = json(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"admin\",\"password\":\"admin-password-123\"}")).andExpect(status().isOk()))
                .path("token").asString();

        mvc.perform(get("/api/admin/system/health").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.database.status").value("UP"))
                .andExpect(jsonPath("$.components.redis.status").value("DISABLED"));
    }

    private JsonNode json(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
