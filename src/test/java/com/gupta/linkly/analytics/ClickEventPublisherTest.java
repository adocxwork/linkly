package com.gupta.linkly.analytics;

import com.gupta.linkly.redis.RedisGateway;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClickEventPublisherTest {

    private static final String BROWSER = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0 Safari/537.36";

    private final StringRedisTemplate template = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final StreamOperations<String, Object, Object> stream = mock(StreamOperations.class);
    private final LocalClickBuffer buffer = new LocalClickBuffer(2);
    private final PipelineStats stats = new PipelineStats();

    private ClickEventPublisher publisher(boolean redisConfigured) {
        when(template.opsForStream()).thenReturn(stream);
        RedisGateway gateway = new RedisGateway(template, redisConfigured, Duration.ofSeconds(30), Clock.systemUTC());
        return new ClickEventPublisher(gateway, buffer, stats, JsonMapper.builder().build(), "clicks", 1000);
    }

    @Test
    void publishesToRedisStreamWhenAvailable() {
        ClickEventPublisher publisher = publisher(true);
        publisher.publish(UUID.randomUUID(), "8.8.8.8", BROWSER);

        verify(stream).add(any(org.springframework.data.redis.connection.stream.MapRecord.class), any());
        assertThat(stats.getPublishedToStream()).hasValue(1);
        assertThat(buffer.size()).isZero();
        assertThat(publisher.consumeNewEventsSignal()).isTrue();
    }

    @Test
    void fallsBackToLocalBufferWhenRedisFails() {
        when(stream.add(any(org.springframework.data.redis.connection.stream.MapRecord.class), any()))
                .thenThrow(new RedisConnectionFailureException("down"));
        publisher(true).publish(UUID.randomUUID(), "8.8.8.8", BROWSER);

        assertThat(buffer.size()).isEqualTo(1);
        assertThat(stats.getPublishedToLocalBuffer()).hasValue(1);
    }

    @Test
    void usesLocalBufferWithoutTouchingRedisWhenNotConfigured() {
        publisher(false).publish(UUID.randomUUID(), "8.8.8.8", BROWSER);
        verifyNoInteractions(stream);
        assertThat(buffer.size()).isEqualTo(1);
    }

    @Test
    void dropsAndCountsWhenLocalBufferIsFull() {
        ClickEventPublisher publisher = publisher(false);
        for (int i = 0; i < 3; i++) {
            publisher.publish(UUID.randomUUID(), "8.8.8.8", BROWSER);
        }
        assertThat(buffer.size()).isEqualTo(2);
        assertThat(stats.getDroppedBufferFull()).hasValue(1);
    }

    @Test
    void ignoresBots() {
        publisher(false).publish(UUID.randomUUID(), "8.8.8.8", "WhatsApp/2.23.20");
        assertThat(buffer.size()).isZero();
        assertThat(stats.getBotClicksIgnored()).hasValue(1);
    }
}
