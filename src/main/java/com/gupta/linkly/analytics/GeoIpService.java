package com.gupta.linkly.analytics;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Resolves IPs to country/city using the free geojs.io API.
 *
 * <ul>
 *   <li>Batched: one HTTP call per batch of clicks (up to 50 IPs), not one per click.</li>
 *   <li>Cached: bounded Caffeine cache (max 50k IPs, 24h), so repeat visitors cost nothing.</li>
 *   <li>Fails soft: on error we record "Unknown" and pause lookups for 5 minutes. Analytics
 *       must never fail just because a third-party API is down.</li>
 * </ul>
 */
@Slf4j
@Service
public class GeoIpService {

    public record Location(String country, String city) {
        public static final Location UNKNOWN = new Location("Unknown", "Unknown");
        public static final Location LOCAL = new Location("Local network", "Local network");
    }

    private static final int MAX_IPS_PER_REQUEST = 50;
    private static final Duration PAUSE_AFTER_FAILURE = Duration.ofMinutes(5);

    private final boolean enabled;
    private final String url;
    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final Cache<String, Location> cache = Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterWrite(Duration.ofHours(24))
            .build();

    private volatile Instant pausedUntil = Instant.EPOCH;
    private volatile Instant lastSuccessAt;
    private volatile Instant lastFailureAt;
    private volatile String lastError;

    public GeoIpService(@Value("${app.geoip.enabled:true}") boolean enabled,
                        @Value("${app.geoip.url:https://get.geojs.io/v1/ip/geo.json}") String url,
                        JsonMapper jsonMapper) {
        this.enabled = enabled;
        this.url = url;
        this.jsonMapper = jsonMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2_000);
        factory.setReadTimeout(3_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /** Returns a location for every input IP (never throws). */
    public Map<String, Location> lookup(Collection<String> ips) {
        Map<String, Location> result = new HashMap<>();
        List<String> toFetch = new ArrayList<>();
        for (String ip : new LinkedHashSet<>(ips)) {
            if (ip == null || IpAddresses.isPrivateOrLocal(ip)) {
                result.put(ip, ip != null && IpAddresses.isValid(ip) ? Location.LOCAL : Location.UNKNOWN);
                continue;
            }
            Location cached = cache.getIfPresent(ip);
            if (cached != null) {
                result.put(ip, cached);
            } else {
                toFetch.add(ip);
            }
        }
        if (!toFetch.isEmpty() && enabled && Instant.now().isAfter(pausedUntil)) {
            for (int i = 0; i < toFetch.size(); i += MAX_IPS_PER_REQUEST) {
                List<String> chunk = toFetch.subList(i, Math.min(i + MAX_IPS_PER_REQUEST, toFetch.size()));
                if (!fetchChunk(chunk, result)) {
                    break;
                }
            }
        }
        for (String ip : toFetch) {
            result.putIfAbsent(ip, Location.UNKNOWN);
        }
        return result;
    }

    private boolean fetchChunk(List<String> ips, Map<String, Location> out) {
        try {
            String body = restClient.get()
                    .uri(url + "?ip={ips}", String.join(",", ips))
                    .retrieve()
                    .body(String.class);
            JsonNode array = jsonMapper.readTree(body == null ? "[]" : body);
            for (JsonNode node : array) {
                String ip = node.path("ip").asString(null);
                if (ip == null) {
                    continue;
                }
                Location location = new Location(text(node, "country"), text(node, "city"));
                cache.put(ip, location);
                out.put(ip, location);
            }
            lastSuccessAt = Instant.now();
            return true;
        } catch (RuntimeException e) {
            lastFailureAt = Instant.now();
            lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            pausedUntil = Instant.now().plus(PAUSE_AFTER_FAILURE);
            log.warn("Geo-IP lookup failed, pausing lookups for {} min: {}", PAUSE_AFTER_FAILURE.toMinutes(), lastError);
            return false;
        }
    }

    private static String text(JsonNode node, String field) {
        String value = node.path(field).asString("");
        return value.isBlank() || "nil".equals(value) ? "Unknown" : value;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPaused() {
        return Instant.now().isBefore(pausedUntil);
    }

    public Instant getLastSuccessAt() {
        return lastSuccessAt;
    }

    public Instant getLastFailureAt() {
        return lastFailureAt;
    }

    public String getLastError() {
        return lastError;
    }

    public long getCacheSize() {
        return cache.estimatedSize();
    }
}
