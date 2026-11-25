package com.gupta.linkly.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Uses the event UUID as the analytics row's primary key. Only rows inserted by this
 * statement contribute to the link counters, including when a Redis acknowledgement is lost.
 */
@Component
@RequiredArgsConstructor
public class ClickBatchWriter {

    private final JdbcTemplate jdbc;
    private final IpHasher ipHasher;

    @Transactional
    public int write(List<ClickEvent> events, Map<String, GeoIpService.Location> locations) {
        if (events.isEmpty()) {
            return 0;
        }
        String values = String.join(", ", Collections.nCopies(events.size(),
                "(?::uuid, ?::uuid, ?::text, ?::text, ?::text, ?::text, ?::text, ?::timestamp)"));
        // Lock the existing links in a consistent order. A concurrent delete either wins before
        // that lock (the event is skipped) or waits until the insert and counter update commit.
        String sql = """
                WITH incoming(id, link_id, ip_hash, country, city, device_type, browser, clicked_at) AS (
                    VALUES %s
                ),
                existing AS (
                    SELECT id FROM links
                    WHERE id IN (SELECT link_id FROM incoming)
                    ORDER BY id FOR NO KEY UPDATE
                ),
                inserted AS (
                    INSERT INTO click_analytics (id, link_id, ip_hash, country, city, device_type, browser, clicked_at)
                    SELECT incoming.* FROM incoming JOIN existing ON existing.id = incoming.link_id
                    ON CONFLICT (id) DO NOTHING
                    RETURNING link_id
                ),
                counts AS (
                    SELECT link_id, count(*) AS delta FROM inserted GROUP BY link_id
                )
                UPDATE links
                SET click_count = click_count + counts.delta
                FROM counts WHERE links.id = counts.link_id
                RETURNING counts.delta
                """.formatted(values);
        List<Integer> counts = jdbc.query(sql, statement -> {
            int index = 1;
            for (ClickEvent event : events) {
                GeoIpService.Location location = event.ip() == null ? GeoIpService.Location.UNKNOWN
                        : locations.getOrDefault(event.ip(), GeoIpService.Location.UNKNOWN);
                statement.setObject(index++, event.eventId());
                statement.setObject(index++, event.linkId());
                statement.setString(index++, ipHasher.hash(event.ip()));
                statement.setString(index++, location.country());
                statement.setString(index++, location.city());
                statement.setString(index++, UserAgentParser.deviceType(event.userAgent()));
                statement.setString(index++, UserAgentParser.browser(event.userAgent()));
                statement.setObject(index++, clickTime(event));
            }
        }, (row, rowNumber) -> row.getInt(1));
        return counts.stream().mapToInt(Integer::intValue).sum();
    }

    private static LocalDateTime clickTime(ClickEvent event) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(event.occurredAtMillis()), ZoneId.systemDefault());
    }
}
