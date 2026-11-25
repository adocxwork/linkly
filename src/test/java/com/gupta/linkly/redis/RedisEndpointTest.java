package com.gupta.linkly.redis;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class RedisEndpointTest {

    @Test
    void usesTlsForUpstashAndPreservesTheTcpPassword() {
        RedisEndpoint endpoint = new RedisEndpoint(" redis://default:demo+part%3A%40:@sample.upstash.io:6379 ");

        assertThat(endpoint.error()).isNull();
        assertThat(endpoint.tls()).isTrue();
        assertThat(endpoint.server().getHostName()).isEqualTo("sample.upstash.io");
        assertThat(endpoint.server().getUsername()).isEqualTo("default");
        assertThat(endpoint.server().getPassword().get()).isEqualTo("demo+part:@:".toCharArray());
    }

    @Test
    void stillSupportsLocalPlaintextAndExplicitTls() {
        RedisEndpoint local = new RedisEndpoint("redis://localhost:6380/2");
        RedisEndpoint secure = new RedisEndpoint("rediss://localhost");

        assertThat(local.error()).isNull();
        assertThat(local.tls()).isFalse();
        assertThat(local.server().getPort()).isEqualTo(6380);
        assertThat(local.server().getDatabase()).isEqualTo(2);
        assertThat(secure.error()).isNull();
        assertThat(secure.tls()).isTrue();
    }

    @Test
    void reportsInvalidConfigurationWithoutConnectingOrExposingCredentials() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        RedisEndpoint endpoint = new RedisEndpoint("https://default:test-secret@sample.upstash.io");
        RedisGateway gateway = new RedisGateway(template, endpoint, Duration.ofSeconds(30));

        assertThat(gateway.isConfigured()).isTrue();
        assertThat(gateway.isAvailable()).isFalse();
        assertThat(gateway.ping().message()).contains("Redis TCP URL").doesNotContain("test-secret");
        verifyNoInteractions(template);
    }

    @Test
    void rejectsMissingUpstashCredentialsAndMalformedDatabasePaths() {
        assertThat(new RedisEndpoint("rediss://sample.upstash.io").error()).contains("Connect");
        assertThat(new RedisEndpoint("redis://localhost/not-a-number").error()).contains("database number");
        assertThat(new RedisEndpoint(" ").configured()).isFalse();
    }
}
