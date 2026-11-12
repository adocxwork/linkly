package com.gupta.linkly.analytics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Fallback queue used when Redis is unavailable or disabled. Bounded so a burst of traffic
 * during a long outage cannot exhaust memory: when full, new clicks are dropped and counted.
 * Trade-off vs the Redis Stream: events here are lost if the process restarts.
 */
@Component
public class LocalClickBuffer {

    private final BlockingQueue<ClickEvent> queue;
    private final int capacity;

    public LocalClickBuffer(@Value("${app.analytics.local-buffer-capacity:10000}") int capacity) {
        this.capacity = capacity;
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    public boolean offer(ClickEvent event) {
        return queue.offer(event);
    }

    public List<ClickEvent> drain(int max) {
        List<ClickEvent> batch = new ArrayList<>(Math.min(max, queue.size()));
        queue.drainTo(batch, max);
        return batch;
    }

    /** Puts a failed batch back (as much as fits). Returns how many could not be re-queued. */
    public int requeue(List<ClickEvent> events) {
        int lost = 0;
        for (ClickEvent event : events) {
            if (!queue.offer(event)) {
                lost++;
            }
        }
        return lost;
    }

    public int size() {
        return queue.size();
    }

    public int capacity() {
        return capacity;
    }
}
