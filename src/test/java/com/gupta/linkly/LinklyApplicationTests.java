package com.gupta.linkly;

import com.gupta.linkly.analytics.ClickBatchWriter;
import com.gupta.linkly.analytics.ClickEvent;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    @Autowired
    private ClickBatchWriter clickWriter;

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

        mvc.perform(head("/r/ada-docs").header("User-Agent", "Mozilla/5.0 (Windows NT 10.0) Chrome/120.0"))
                .andExpect(status().isFound());

        mvc.perform(get("/r/ada-docs").header("User-Agent", "Mozilla/5.0 (Windows NT 10.0) Chrome/120.0"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/docs"));

        mvc.perform(get("/api/public/u/ada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links[0].title").value("Docs"))
                .andExpect(jsonPath("$.links[0].clickCount").doesNotExist());

        for (String content : List.of("First", "Second")) {
            mvc.perform(post("/api/public/u/ada/messages").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"" + content + "\"}"))
                    .andExpect(status().isCreated());
        }
        JsonNode newest = json(mvc.perform(get("/api/messages").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].content").value("Second"))).get(0);
        mvc.perform(get("/api/messages").header("Authorization", "Bearer " + token)
                        .param("before", newest.path("createdAt").asString())
                        .param("beforeId", newest.path("id").asString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("First"));

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
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post").exists());
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
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

    @Test
    void clickReplayAndOwnerEditPreserveTheCounter() throws Exception {
        String token = register("replay");
        String id = json(mvc.perform(post("/api/links").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"title":"Demo","originalUrl":"https://example.com","customAlias":"replay-link"}
                                """))
                .andExpect(status().isCreated())).path("id").asString();
        ClickEvent event = new ClickEvent(UUID.randomUUID(), UUID.fromString(id), "8.8.8.8",
                "Mozilla/5.0", System.currentTimeMillis());
        assertThat(clickWriter.write(List.of(event, event), Map.of())).isEqualTo(1);
        assertThat(clickWriter.write(List.of(event), Map.of())).isZero();

        mvc.perform(put("/api/links/" + id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"title":"Edited","originalUrl":"https://example.com","active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount").value(1));
        mvc.perform(get("/api/links/" + id + "/analytics").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(1))
                .andExpect(jsonPath("$.uniqueVisitors").value(1));
        mvc.perform(get("/r/replay-link")).andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost:5173/link-error"));
    }

    @Test
    void deletingAndRecreatingUsernameDoesNotRestoreOldSession() throws Exception {
        String previousToken = register("reusable");
        mvc.perform(delete("/api/users/me").header("Authorization", "Bearer " + previousToken))
                .andExpect(status().isNoContent());
        String newToken = register("reusable");
        mvc.perform(get("/api/links").header("Authorization", "Bearer " + previousToken))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/links").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk());
    }

    @Test
    void redirectsUnicodeUrlsAndKeepsLinkAndInboxAccessScopedToTheOwner() throws Exception {
        String ownerToken = register("owner");
        String otherToken = register("visitor");
        String linkId = json(mvc.perform(post("/api/links").header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"title":"Unicode","originalUrl":"https://example.com/café/📚?topic=résumé","customAlias":"unicode-docs"}
                                """))
                .andExpect(status().isCreated())).path("id").asString();

        mvc.perform(head("/r/unicode-docs")).andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/caf%C3%A9/%F0%9F%93%9A?topic=r%C3%A9sum%C3%A9"));
        mvc.perform(get("/api/links/" + linkId + "/analytics").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/links/" + linkId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mvc.perform(put("/api/users/settings").header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enablePublicMessaging\":true}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/public/u/owner/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello Linkly\"}"))
                .andExpect(status().isCreated());
        String messageId = json(mvc.perform(get("/api/messages").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())).get(0).path("id").asString();
        mvc.perform(delete("/api/messages/" + messageId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/messages/" + messageId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
    }

    private String register(String username) throws Exception {
        return json(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Example","username":"%s","email":"%s@example.com","password":"password123"}
                        """.formatted(username, username))).andExpect(status().isCreated())).path("token").asString();
    }

    private JsonNode json(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
