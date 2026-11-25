package com.gupta.linkly.analytics;

import com.gupta.linkly.redis.RedisGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Background consumer for click events.
 *
 * <h3>Why not a blocking StreamMessageListenerContainer?</h3>
 * The previous version polled Redis every second, forever: ~86,000 commands/day even with zero
 * traffic, which exhausts Upstash's free quota. This worker only reads the stream when:
 * <ul>
 *   <li>this instance published something since the last run (an in-memory flag, free to check), or</li>
 *   <li>a periodic sweep is due (default every 5 min) to pick up leftovers, e.g. events that
 *       were published just before a restart.</li>
 * </ul>
 * Idle cost: a couple of commands per sweep, ~600/day instead of ~86,000.
 *
 * <h3>Delivery guarantees (at-least-once)</h3>
 * Records are read through a consumer group and acknowledged (XACK) only after the database
 * transaction commits. If processing fails the records stay "pending" and are re-read next time
 * (we read our pending list with ID 0 before reading new records with ID &gt;).
 * Malformed records are acknowledged and counted. Database failures pause consumption briefly.
 */
@Slf4j
@Component
public class ClickStreamWorker {

    private final RedisGateway redis;
    private final ClickEventPublisher publisher;
    private final LocalClickBuffer localBuffer;
    private final ClickProcessor processor;
    private final PipelineStats stats;
    private final JsonMapper jsonMapper;

    private final String streamKey;
    private final String group;
    private final String consumerName;
    private final int batchSize;
    private final Duration sweepInterval;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(30);

    private volatile boolean groupReady = false;
    private volatile Instant nextSweepAt = Instant.EPOCH;
    private Instant retryStreamAt = Instant.EPOCH;
    private Instant retryLocalAt = Instant.EPOCH;
    private volatile Instant lastDbErrorLogAt = Instant.EPOCH;

    public ClickStreamWorker(RedisGateway redis, ClickEventPublisher publisher, LocalClickBuffer localBuffer,
                             ClickProcessor processor, PipelineStats stats, JsonMapper jsonMapper,
                             @Value("${app.analytics.consumer-group:analytics-group}") String group,
                             @Value("${app.analytics.consumer-name:linkly-consumer}") String consumerName,
                             @Value("${app.analytics.batch-size:100}") int batchSize,
                             @Value("${app.analytics.sweep-interval:5m}") Duration sweepInterval) {
        this.redis = redis;
        this.publisher = publisher;
        this.localBuffer = localBuffer;
        this.processor = processor;
        this.stats = stats;
        this.jsonMapper = jsonMapper;
        this.streamKey = publisher.streamKey();
        this.group = group;
        this.consumerName = consumerName;
        this.batchSize = batchSize;
        this.sweepInterval = sweepInterval;
    }

    @Scheduled(initialDelay = 5_000, fixedDelayString = "${app.analytics.drain-interval-ms:2000}")
    public void tick() {
        drainLocalBuffer();
        drainStream();
    }

    // ------------------------------------------------------------------ in-memory fallback queue

    void drainLocalBuffer() {
        if (Instant.now().isBefore(retryLocalAt)) {
            return;
        }
        List<ClickEvent> batch = localBuffer.drain(batchSize);
        if (batch.isEmpty()) {
            return;
        }
        try {
            processor.process(batch);
        } catch (RuntimeException e) {
            int lost = localBuffer.requeue(batch);
            stats.getDroppedBufferFull().addAndGet(lost);
            retryLocalAt = Instant.now().plus(RETRY_DELAY);
            onProcessingError("local buffer", e);
        }
    }

    // ------------------------------------------------------------------ Redis Stream

    void drainStream() {
        Instant now = Instant.now();
        if (now.isBefore(retryStreamAt) || !redis.isAvailable()) {
            return;
        }
        boolean sweepDue = !now.isBefore(nextSweepAt);
        if (!publisher.consumeNewEventsSignal() && !sweepDue) {
            return;
        }
        if (sweepDue) {
            nextSweepAt = now.plus(sweepInterval);
        }
        if (!ensureGroup()) {
            publisher.signalMoreWork();
            return;
        }
        int pending = processRecords(read(ReadOffset.from("0")));
        if (pending < 0 || pending >= batchSize || !groupReady || !redis.isAvailable()) {
            publisher.signalMoreWork();
            return;
        }
        int fresh = processRecords(read(ReadOffset.lastConsumed()));
        if (fresh < 0 || fresh >= batchSize || !groupReady || !redis.isAvailable()) {
            publisher.signalMoreWork();
        }
    }

    private boolean ensureGroup() {
        if (groupReady) {
            return true;
        }
        Optional<String> result = redis.execute("XGROUP CREATE", t -> t.execute((RedisCallback<String>) connection -> {
            try {
                return createGroup(connection);
            } catch (RuntimeException e) {
                if (redisErrorContains(e, "BUSYGROUP")) {
                    return "EXISTS"; // group already there: fine
                }
                throw e;
            }
        }));
        groupReady = result.isPresent();
        return groupReady;
    }

    private String createGroup(RedisConnection connection) {
        // MKSTREAM: create the stream if nobody has published yet. Offset 0 = consume from the start.
        return connection.streamCommands().xGroupCreate(
                streamKey.getBytes(StandardCharsets.UTF_8), group, ReadOffset.from("0-0"), true);
    }

    private List<MapRecord<String, Object, Object>> read(ReadOffset offset) {
        return redis.execute("XREADGROUP", t -> {
            try {
                return t.opsForStream().read(
                        Consumer.from(group, consumerName),
                        StreamReadOptions.empty().count(batchSize),
                        StreamOffset.create(streamKey, offset));
            } catch (RuntimeException e) {
                if (redisErrorContains(e, "NOGROUP")) {
                    groupReady = false; // stream/group was deleted (e.g. Redis flushed): recreate next time
                    return List.<MapRecord<String, Object, Object>>of();
                }
                throw e;
            }
        }).orElse(List.of());
    }

    private int processRecords(List<MapRecord<String, Object, Object>> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }
        List<ClickEvent> events = new ArrayList<>();
        List<RecordId> ids = new ArrayList<>();
        List<RecordId> malformed = new ArrayList<>();
        for (MapRecord<String, Object, Object> record : records) {
            Object payload = record.getValue().get("payload");
            try {
                ClickEvent event = jsonMapper.readValue(String.valueOf(payload), ClickEvent.class);
                if (event == null || event.linkId() == null) {
                    malformed.add(record.getId());
                    continue;
                }
                Long occurredAt = event.occurredAtMillis();
                if (event.eventId() == null || occurredAt == null || occurredAt <= 0) {
                    // Older deployments omitted the event ID and sometimes the timestamp.
                    String legacyKey = streamKey + ":" + record.getId().getValue();
                    UUID id = event.eventId() == null
                            ? UUID.nameUUIDFromBytes(legacyKey.getBytes(StandardCharsets.UTF_8)) : event.eventId();
                    long timestamp = occurredAt != null && occurredAt > 0 ? occurredAt
                            : Long.parseLong(record.getId().getValue().split("-", 2)[0]);
                    event = new ClickEvent(id, event.linkId(), event.ip(), event.userAgent(), timestamp);
                }
                events.add(event);
                ids.add(record.getId());
            } catch (RuntimeException e) {
                malformed.add(record.getId());
            }
        }
        if (!malformed.isEmpty() && !acknowledge(malformed)) {
            return -1;
        }
        stats.getDeadLettered().addAndGet(malformed.size());
        if (ids.isEmpty()) {
            return records.size();
        }
        try {
            processor.process(events);
        } catch (RuntimeException e) {
            onProcessingError("stream", e);
            retryStreamAt = Instant.now().plus(RETRY_DELAY);
            return -1;
        }
        return acknowledge(ids) ? records.size() : -1;
    }

    private boolean acknowledge(List<RecordId> ids) {
        return redis.execute("XACK", t -> t.opsForStream().acknowledge(streamKey, group, ids.toArray(RecordId[]::new))).isPresent();
    }

    private static boolean redisErrorContains(Throwable error, String marker) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (String.valueOf(cause.getMessage()).contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private void onProcessingError(String source, RuntimeException e) {
        stats.recordError(source + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
        // Avoid flooding logs every 2 seconds while the database is down.
        if (Instant.now().isAfter(lastDbErrorLogAt.plusSeconds(60))) {
            lastDbErrorLogAt = Instant.now();
            log.error("Failed to store click batch from {} (will retry): {}", source, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ health info

    public record StreamInfo(Long length, Long pending) {
    }

    /** Reads stream length and pending count for the admin panel (2 Redis commands). */
    public StreamInfo streamInfo() {
        Long length = redis.execute("XLEN", t -> t.opsForStream().size(streamKey)).orElse(null);
        Long pending = groupReady
                ? redis.execute("XPENDING", t -> t.opsForStream().pending(streamKey, group).getTotalPendingMessages()).orElse(null)
                : null;
        return new StreamInfo(length, pending);
    }

    public boolean isGroupReady() {
        return groupReady;
    }
}
