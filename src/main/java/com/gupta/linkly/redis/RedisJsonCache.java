package com.gupta.linkly.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;

/**
 * Cache-aside helper on top of {@link RedisGateway}: values are stored as JSON strings.
 * A cache problem (Redis down, corrupt entry) is always treated as a cache miss.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisJsonCache {

    private final RedisGateway redis;
    private final JsonMapper jsonMapper;

    public <T> Optional<T> get(String key, Class<T> type) {
        Optional<String> raw = redis.execute("GET", t -> t.opsForValue().get(key));
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(jsonMapper.readValue(raw.get(), type));
        } catch (RuntimeException e) {
            log.warn("Dropping unreadable cache entry {}: {}", key, e.getMessage());
            redis.run("DEL", t -> t.delete(key));
            return Optional.empty();
        }
    }

    public void put(String key, Object value, Duration ttl) {
        String json;
        try {
            json = jsonMapper.writeValueAsString(value);
        } catch (RuntimeException e) {
            log.warn("Could not serialise cache value for {}: {}", key, e.getMessage());
            return;
        }
        redis.run("SET", t -> t.opsForValue().set(key, json, ttl));
    }

    /**
     * Evicts keys once the current database transaction has committed (immediately if there is
     * none). Evicting before commit could let another request re-cache the old value.
     */
    public void evictAfterCommit(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        Runnable evict = () -> redis.run("DEL", t -> t.delete(keys));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict.run();
                }
            });
        } else {
            evict.run();
        }
    }
}
