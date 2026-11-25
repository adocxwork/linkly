package com.gupta.linkly.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The only class that talks to Redis. Every caller gets an "it worked / it didn't" answer and
 * must have a fallback, so Redis is an optimisation, never a hard dependency.
 *
 * <p>Three things decide whether Redis is used:
 * <ol>
 *   <li><b>configured</b>: REDIS_URL is set. If not, the app runs without Redis entirely.</li>
 *   <li><b>enabled</b>: the admin switch in the dashboard (persisted in system_settings).</li>
 *   <li><b>circuit breaker</b>: after a failure we stop calling Redis for a cool-down period.
 *       After the cool-down the next call is a trial: success closes the circuit, failure
 *       re-opens it.</li>
 * </ol>
 */
@Slf4j
@Component
public class RedisGateway {

    private final StringRedisTemplate template;
    private final boolean configured;
    private final String configurationError;
    private final Duration cooldown;
    private final Clock clock;

    private volatile boolean enabled = true;
    private volatile Instant circuitOpenUntil = Instant.EPOCH;
    private final AtomicLong consecutiveFailures = new AtomicLong();
    private volatile String lastError;
    private volatile Instant lastErrorAt;
    private volatile Instant lastSuccessAt;

    @Autowired
    public RedisGateway(StringRedisTemplate template,
                        RedisEndpoint endpoint,
                        @Value("${app.redis.circuit-cooldown:30s}") Duration cooldown) {
        this(template, endpoint.configured(), endpoint.error(), cooldown, Clock.systemUTC());
    }

    /** Visible for tests: lets them control "configured" and the clock. */
    public RedisGateway(StringRedisTemplate template, boolean configured, Duration cooldown, Clock clock) {
        this(template, configured, null, cooldown, clock);
    }

    private RedisGateway(StringRedisTemplate template, boolean configured, String configurationError,
                         Duration cooldown, Clock clock) {
        this.template = template;
        this.configurationError = configurationError;
        this.configured = configured;
        this.cooldown = cooldown;
        this.clock = clock;
        if (configurationError != null) {
            log.warn("Redis configuration invalid: {} Clicks use the in-memory queue.", configurationError);
        } else if (!configured) {
            log.warn("REDIS_URL is not set: running without Redis (clicks processed in-memory).");
        }
    }

    // ---------------------------------------------------------------- state

    public boolean isConfigured() {
        return configured;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Called by SystemSettingsService when the admin flips the switch. */
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        if (enabled) {
            circuitOpenUntil = Instant.EPOCH;
            consecutiveFailures.set(0);
        }
        this.enabled = enabled;
        log.info("Redis usage {} by admin setting", enabled ? "ENABLED" : "DISABLED");
    }

    public boolean isCircuitOpen() {
        return clock.instant().isBefore(circuitOpenUntil);
    }

    /** True when callers should try Redis right now. */
    public boolean isAvailable() {
        return configured && configurationError == null && enabled && !isCircuitOpen();
    }

    public long getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getLastErrorAt() {
        return lastErrorAt;
    }

    public Instant getLastSuccessAt() {
        return lastSuccessAt;
    }

    public Instant getCircuitOpenUntil() {
        return isCircuitOpen() ? circuitOpenUntil : null;
    }

    // ---------------------------------------------------------------- operations

    /**
     * Runs a Redis operation if Redis is available. Returns empty when Redis is skipped or fails,
     * and also when the operation itself returned null (e.g. a cache miss). Never throws.
     */
    public <T> Optional<T> execute(String operation, Function<StringRedisTemplate, T> action) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            T result = action.apply(template);
            onSuccess();
            return Optional.ofNullable(result);
        } catch (RuntimeException e) {
            onFailure(operation, e);
            return Optional.empty();
        }
    }

    /** Like {@link #execute} for operations without a result. Returns true if it ran successfully. */
    public boolean run(String operation, Consumer<StringRedisTemplate> action) {
        if (!isAvailable()) {
            return false;
        }
        try {
            action.accept(template);
            onSuccess();
            return true;
        } catch (RuntimeException e) {
            onFailure(operation, e);
            return false;
        }
    }

    /**
     * Health probe used by the admin dashboard. Ignores the circuit breaker (so the admin can see
     * whether Redis has recovered) but respects "configured" and "enabled".
     */
    public PingResult ping() {
        if (!configured) {
            return new PingResult(false, null, "REDIS_URL not configured");
        }
        if (!enabled) {
            return new PingResult(false, null, "Disabled by admin");
        }
        if (configurationError != null) {
            return new PingResult(false, null, configurationError);
        }
        long start = System.nanoTime();
        try {
            String reply = template.execute((org.springframework.data.redis.core.RedisCallback<String>)
                    connection -> connection.ping());
            long latency = Duration.ofNanos(System.nanoTime() - start).toMillis();
            if (!"PONG".equalsIgnoreCase(reply)) {
                throw new IllegalStateException("Redis did not return PONG");
            }
            return new PingResult(true, latency, reply);
        } catch (RuntimeException e) {
            onFailure("PING", e);
            return new PingResult(false, null, rootMessage(e));
        }
    }

    public record PingResult(boolean ok, Long latencyMs, String message) {
    }

    // ---------------------------------------------------------------- breaker

    private void onSuccess() {
        lastSuccessAt = clock.instant();
        if (consecutiveFailures.getAndSet(0) > 0) {
            log.info("Redis is reachable again; circuit closed.");
        }
        circuitOpenUntil = Instant.EPOCH;
    }

    private void onFailure(String operation, RuntimeException e) {
        long failures = consecutiveFailures.incrementAndGet();
        String explanation = rootMessage(e);
        lastError = operation + ": " + explanation;
        lastErrorAt = clock.instant();
        circuitOpenUntil = clock.instant().plus(cooldown);
        if (failures == 1) {
            log.warn("Redis {} failed, skipping Redis for {}s: {}", operation, cooldown.toSeconds(), explanation);
        } else {
            log.debug("Redis {} failed again ({} consecutive failures)", operation, failures);
        }
    }

    static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String msg = t.getMessage();
        if (msg != null) {
            msg = msg.replaceAll("(?i)rediss?://[^\\s)]+", "[Redis URL redacted]");
        }
        return t.getClass().getSimpleName() + (msg != null ? ": " + msg : "");
    }
}
