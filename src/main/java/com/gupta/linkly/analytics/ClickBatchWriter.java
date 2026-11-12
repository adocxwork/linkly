package com.gupta.linkly.analytics;

import com.gupta.linkly.entity.ClickAnalytics;
import com.gupta.linkly.repository.ClickAnalyticsRepository;
import com.gupta.linkly.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Writes a batch of clicks in ONE database transaction:
 * a batched INSERT of analytics rows plus one UPDATE per link (click_count + n).
 * Compared to one transaction per click this cuts database round-trips by roughly the batch size.
 */
@Component
@RequiredArgsConstructor
public class ClickBatchWriter {

    private final LinkRepository linkRepository;
    private final ClickAnalyticsRepository analyticsRepository;
    private final IpHasher ipHasher;

    /** Events published by the previous app version carry no timestamp: use "now" for those. */
    private static LocalDateTime clickTime(ClickEvent event) {
        long millis = event.occurredAtMillis() > 0 ? event.occurredAtMillis() : System.currentTimeMillis();
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }

    /** @return number of clicks written (events for deleted links are skipped). */
    @Transactional
    public int write(List<ClickEvent> events, Map<String, GeoIpService.Location> locations) {
        Set<UUID> linkIds = events.stream().map(ClickEvent::linkId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (linkIds.isEmpty()) {
            return 0;
        }
        // A link may have been deleted after it was clicked; inserting would violate the FK.
        Set<UUID> existing = new HashSet<>(linkRepository.findExistingIds(linkIds));

        List<ClickAnalytics> rows = new ArrayList<>();
        Map<UUID, Long> countsPerLink = new HashMap<>();
        for (ClickEvent event : events) {
            if (event.linkId() == null || !existing.contains(event.linkId())) {
                continue;
            }
            GeoIpService.Location location = locations.getOrDefault(event.ip(), GeoIpService.Location.UNKNOWN);
            rows.add(ClickAnalytics.builder()
                    .link(linkRepository.getReferenceById(event.linkId()))
                    .ipHash(ipHasher.hash(event.ip()))
                    .country(location.country())
                    .city(location.city())
                    .deviceType(UserAgentParser.deviceType(event.userAgent()))
                    .browser(UserAgentParser.browser(event.userAgent()))
                    .clickedAt(clickTime(event))
                    .build());
            countsPerLink.merge(event.linkId(), 1L, Long::sum);
        }
        analyticsRepository.saveAll(rows);
        countsPerLink.forEach(linkRepository::incrementClickCount);
        return rows.size();
    }
}
