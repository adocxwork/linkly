package com.gupta.linkly.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisGatewayTest {

    private final StringRedisTemplate template = mock(StringRedisTemplate.class);
    private MutableClock clock;
    private RedisGateway gateway;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        gateway = new RedisGateway(template, true, Duration.ofSeconds(30), clock);
    }

    @Test
    void returnsResultWhenRedisWorks() {
        assertThat(gateway.execute("GET", t -> "value")).contains("value");
        assertThat(gateway.isAvailable()).isTrue();
    }

    @Test
    void failureOpensCircuitAndLaterCallsAreSkipped() {
        AtomicInteger calls = new AtomicInteger();
        gateway.execute("GET", t -> {
            calls.incrementAndGet();
            throw new RedisConnectionFailureException("down");
        });

        assertThat(gateway.isCircuitOpen()).isTrue();
        assertThat(gateway.execute("GET", t -> calls.incrementAndGet())).isEmpty();
        assertThat(gateway.run("SET", t -> calls.incrementAndGet())).isFalse();
        assertThat(calls).hasValue(1); // the skipped calls never reached Redis
        assertThat(gateway.getLastError()).contains("GET").contains("down");
    }

    @Test
    void circuitAllowsATrialCallAfterCooldownAndClosesOnSuccess() {
        gateway.run("GET", t -> { throw new RedisConnectionFailureException("down"); });
        clock.advance(Duration.ofSeconds(31));

        assertThat(gateway.isAvailable()).isTrue();
        assertThat(gateway.execute("GET", t -> "ok")).contains("ok");
        assertThat(gateway.getConsecutiveFailures()).isZero();
    }

    @Test
    void neverCallsRedisWhenNotConfigured() {
        RedisGateway unconfigured = new RedisGateway(template, false, Duration.ofSeconds(30), clock);
        AtomicInteger calls = new AtomicInteger();
        assertThat(unconfigured.execute("GET", t -> calls.incrementAndGet())).isEmpty();
        assertThat(calls).hasValue(0);
        assertThat(unconfigured.ping().ok()).isFalse();
    }

    @Test
    void adminSwitchDisablesAndReenablingResetsTheBreaker() {
        gateway.run("GET", t -> { throw new RedisConnectionFailureException("down"); });
        gateway.setEnabled(false);
        assertThat(gateway.isAvailable()).isFalse();
        assertThat(gateway.ping().message()).isEqualTo("Disabled by admin");

        gateway.setEnabled(true);
        assertThat(gateway.isAvailable()).isTrue(); // breaker reset, no need to wait for cooldown
    }

    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
