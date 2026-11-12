package com.gupta.linkly.analytics;

import com.gupta.linkly.redis.RedisGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Called on the redirect hot path. It must be fast and must never fail the redirect.
 *
 * <p>Preferred path: append the click to a Redis Stream (durable across app restarts, consumed
 * by {@link ClickStreamWorker}). The stream is capped with an approximate MAXLEN so it cannot
 * grow forever in Upstash. Fallback path: the bounded in-memory {@link LocalClickBuffer}.
 */
@Slf4j
@Component
public class ClickEventPublisher {

    private final RedisGateway redis;
    private final LocalClickBuffer localBuffer;
    private final PipelineStats stats;
    private final JsonMapper jsonMapper;
    private final String streamKey;
    private final long maxStreamLength;

    /** Set when we publish to the stream so the worker only reads Redis when there is work. */
    private final AtomicBoolean streamHasNewEvents = new AtomicBoolean(false);

    public ClickEventPublisher(RedisGateway redis, LocalClickBuffer localBuffer, PipelineStats stats, JsonMapper jsonMapper,
                               @Value("${app.analytics.stream-key:link-clicks-stream}") String streamKey,
                               @Value("${app.analytics.stream-max-length:10000}") long maxStreamLength) {
        this.redis = redis;
        this.localBuffer = localBuffer;
        this.stats = stats;
        this.jsonMapper = jsonMapper;
        this.streamKey = streamKey;
        this.maxStreamLength = maxStreamLength;
    }

    public void publish(UUID linkId, String ip, String userAgent) {
        if (UserAgentParser.isBot(userAgent)) {
            stats.getBotClicksIgnored().incrementAndGet();
            return;
        }
        ClickEvent event = new ClickEvent(linkId, ip, truncate(userAgent, 512), System.currentTimeMillis());

        if (redis.isAvailable()) {
            String payload = jsonMapper.writeValueAsString(event);
            boolean published = redis.run("XADD", t -> t.opsForStream().add(
                    StreamRecords.newRecord().in(streamKey).ofMap(Map.of("payload", payload)),
                    XAddOptions.maxlen(maxStreamLength).approximateTrimming(true)));
            if (published) {
                stats.getPublishedToStream().incrementAndGet();
                streamHasNewEvents.set(true);
                return;
            }
        }

        if (localBuffer.offer(event)) {
            stats.getPublishedToLocalBuffer().incrementAndGet();
        } else {
            stats.getDroppedBufferFull().incrementAndGet();
            log.warn("Local click buffer full ({}), dropping click event", localBuffer.capacity());
        }
    }

    /** Returns true (and resets the flag) if events were published since the last call. */
    boolean consumeNewEventsSignal() {
        return streamHasNewEvents.getAndSet(false);
    }

    void signalMoreWork() {
        streamHasNewEvents.set(true);
    }

    String streamKey() {
        return streamKey;
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
