package com.gupta.linkly.analytics;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/** Counters shown on the admin System Health panel. In-memory, reset on restart. */
@Getter
@Component
public class PipelineStats {

    private final AtomicLong publishedToStream = new AtomicLong();
    private final AtomicLong publishedToLocalBuffer = new AtomicLong();
    private final AtomicLong processed = new AtomicLong();
    private final AtomicLong skippedDeletedLinks = new AtomicLong();
    private final AtomicLong droppedBufferFull = new AtomicLong();
    private final AtomicLong deadLettered = new AtomicLong();
    private final AtomicLong botClicksIgnored = new AtomicLong();
    private volatile Instant lastProcessedAt;
    private volatile Instant lastErrorAt;
    private volatile String lastError;

    public void recordProcessed(int written, int skipped) {
        processed.addAndGet(written);
        skippedDeletedLinks.addAndGet(skipped);
        lastProcessedAt = Instant.now();
    }

    public void recordError(String error) {
        lastError = error;
        lastErrorAt = Instant.now();
    }
}
