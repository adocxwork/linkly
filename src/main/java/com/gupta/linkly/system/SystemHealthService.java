package com.gupta.linkly.system;

import com.gupta.linkly.analytics.ClickStreamWorker;
import com.gupta.linkly.analytics.GeoIpService;
import com.gupta.linkly.analytics.LocalClickBuffer;
import com.gupta.linkly.analytics.PipelineStats;
import com.gupta.linkly.redis.RedisGateway;
import com.gupta.linkly.web.ClientIpResolver;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the admin "System Health" report: live checks of PostgreSQL and Redis plus the state of
 * the click pipeline, Geo-IP API, self-ping and the JVM.
 */
@Service
@RequiredArgsConstructor
public class SystemHealthService {

    public enum Status { UP, DEGRADED, DOWN, DISABLED }

    public record ComponentHealth(Status status, Long latencyMs, Map<String, Object> details) {
    }

    public record HealthReport(Status status, Instant checkedAt, Map<String, ComponentHealth> components,
                               Map<String, Object> requestInfo) {
    }

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final RedisGateway redis;
    private final ClickStreamWorker streamWorker;
    private final LocalClickBuffer localBuffer;
    private final PipelineStats stats;
    private final GeoIpService geoIpService;
    private final KeepAliveService keepAliveService;
    private final SystemSettingsService settingsService;
    private final ClientIpResolver clientIpResolver;

    public HealthReport check(HttpServletRequest request) {
        Map<String, ComponentHealth> components = new LinkedHashMap<>();
        components.put("database", database());
        components.put("redis", redis());
        components.put("clickPipeline", clickPipeline());
        components.put("geoIp", geoIp());
        components.put("keepAlive", keepAlive());
        components.put("jvm", jvm());

        Status overall = Status.UP;
        if (components.get("database").status() == Status.DOWN) {
            overall = Status.DOWN; // nothing works without the database
        } else if (components.values().stream().anyMatch(c -> c.status() == Status.DOWN || c.status() == Status.DEGRADED)) {
            overall = Status.DEGRADED; // core features work, an optional service is impaired
        }
        return new HealthReport(overall, Instant.now(), components, requestInfo(request));
    }

    private ComponentHealth database() {
        Map<String, Object> details = new LinkedHashMap<>();
        long start = System.nanoTime();
        try {
            String version = jdbcTemplate.queryForObject("select version()", String.class);
            long latency = millisSince(start);
            details.put("version", version != null && version.length() > 60 ? version.substring(0, 60) + "…" : version);
            if (dataSource instanceof HikariDataSource hikari && hikari.getHikariPoolMXBean() != null) {
                HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
                details.put("poolActive", pool.getActiveConnections());
                details.put("poolIdle", pool.getIdleConnections());
                details.put("poolMax", hikari.getMaximumPoolSize());
                details.put("threadsWaiting", pool.getThreadsAwaitingConnection());
            }
            return new ComponentHealth(Status.UP, latency, details);
        } catch (RuntimeException e) {
            details.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            return new ComponentHealth(Status.DOWN, null, details);
        }
    }

    private ComponentHealth redis() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("configured", redis.isConfigured());
        details.put("enabledByAdmin", redis.isEnabled());
        if (!redis.isConfigured() || !redis.isEnabled()) {
            details.put("note", !redis.isConfigured() ? "REDIS_URL not set" : "Turned off in admin settings");
            details.put("effect", "No caching; clicks use the in-memory queue");
            return new ComponentHealth(Status.DISABLED, null, details);
        }
        RedisGateway.PingResult ping = redis.ping();
        details.put("circuitOpenUntil", redis.getCircuitOpenUntil());
        details.put("consecutiveFailures", redis.getConsecutiveFailures());
        details.put("lastSuccessAt", redis.getLastSuccessAt());
        details.put("lastError", redis.getLastError());
        details.put("lastErrorAt", redis.getLastErrorAt());
        if (!ping.ok()) {
            details.put("error", ping.message());
            details.put("effect", "Falling back to PostgreSQL and the in-memory click queue");
            return new ComponentHealth(Status.DOWN, null, details);
        }
        return new ComponentHealth(Status.UP, ping.latencyMs(), details);
    }

    private ComponentHealth clickPipeline() {
        Map<String, Object> details = new LinkedHashMap<>();
        boolean usingRedis = redis.isAvailable();
        details.put("mode", usingRedis ? "Redis Stream" : "In-memory fallback queue");
        if (usingRedis) {
            ClickStreamWorker.StreamInfo info = streamWorker.streamInfo();
            details.put("streamLength", info.length());
            details.put("streamPending", info.pending());
        }
        details.put("localBufferSize", localBuffer.size());
        details.put("localBufferCapacity", localBuffer.capacity());
        details.put("publishedToStream", stats.getPublishedToStream().get());
        details.put("publishedToLocalBuffer", stats.getPublishedToLocalBuffer().get());
        details.put("processed", stats.getProcessed().get());
        details.put("botClicksIgnored", stats.getBotClicksIgnored().get());
        details.put("skippedDeletedLinks", stats.getSkippedDeletedLinks().get());
        details.put("droppedBufferFull", stats.getDroppedBufferFull().get());
        details.put("deadLettered", stats.getDeadLettered().get());
        details.put("lastProcessedAt", stats.getLastProcessedAt());
        details.put("lastError", stats.getLastError());
        details.put("lastErrorAt", stats.getLastErrorAt());

        boolean recentError = stats.getLastErrorAt() != null
                && (stats.getLastProcessedAt() == null || stats.getLastErrorAt().isAfter(stats.getLastProcessedAt()));
        boolean bufferNearlyFull = localBuffer.size() > localBuffer.capacity() * 0.8;
        return new ComponentHealth(recentError || bufferNearlyFull ? Status.DEGRADED : Status.UP, null, details);
    }

    private ComponentHealth geoIp() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("provider", "geojs.io");
        details.put("cachedIps", geoIpService.getCacheSize());
        details.put("lastSuccessAt", geoIpService.getLastSuccessAt());
        details.put("lastFailureAt", geoIpService.getLastFailureAt());
        details.put("lastError", geoIpService.getLastError());
        if (!geoIpService.isEnabled()) {
            return new ComponentHealth(Status.DISABLED, null, details);
        }
        details.put("pausedAfterFailure", geoIpService.isPaused());
        return new ComponentHealth(geoIpService.isPaused() ? Status.DEGRADED : Status.UP, null, details);
    }

    private ComponentHealth keepAlive() {
        Map<String, Object> details = new LinkedHashMap<>();
        boolean enabled = settingsService.get().keepAliveEnabled();
        details.put("enabled", enabled);
        details.put("targetUrl", keepAliveService.getTargetUrl());
        details.put("lastPingAt", keepAliveService.getLastPingAt());
        details.put("lastPingOk", keepAliveService.getLastPingOk());
        details.put("lastMessage", keepAliveService.getLastPingMessage());
        if (!enabled) {
            return new ComponentHealth(Status.DISABLED, null, details);
        }
        boolean failing = Boolean.FALSE.equals(keepAliveService.getLastPingOk());
        return new ComponentHealth(failing ? Status.DEGRADED : Status.UP, null, details);
    }

    private ComponentHealth jvm() {
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long maxMb = rt.maxMemory() / (1024 * 1024);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("heapUsedMb", usedMb);
        details.put("heapMaxMb", maxMb);
        details.put("uptime", formatDuration(Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime())));
        details.put("javaVersion", System.getProperty("java.version"));
        details.put("processors", rt.availableProcessors());
        details.put("threads", Thread.activeCount());
        return new ComponentHealth(usedMb > maxMb * 0.9 ? Status.DEGRADED : Status.UP, null, details);
    }

    /** Helps choose TRUSTED_PROXY_COUNT: compare resolvedClientIp with your real public IP. */
    private Map<String, Object> requestInfo(HttpServletRequest request) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("remoteAddr", request.getRemoteAddr());
        info.put("xForwardedFor", request.getHeader("X-Forwarded-For"));
        info.put("trustedProxyCount", clientIpResolver.getTrustedProxyCount());
        info.put("resolvedClientIp", clientIpResolver.resolve(request));
        return info;
    }

    private static long millisSince(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }

    private static String formatDuration(Duration d) {
        return String.format("%dd %dh %dm", d.toDays(), d.toHoursPart(), d.toMinutesPart());
    }
}
