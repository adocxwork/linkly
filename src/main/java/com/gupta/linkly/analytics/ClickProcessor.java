package com.gupta.linkly.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Turns a batch of raw click events into stored analytics. The slow, unreliable part (HTTP call
 * to the Geo-IP API) happens BEFORE the database transaction opens, so we never hold a
 * database connection while waiting on a third party.
 */
@Component
@RequiredArgsConstructor
public class ClickProcessor {

    private final GeoIpService geoIpService;
    private final ClickBatchWriter writer;
    private final PipelineStats stats;

    /** Throws if the batch could not be stored, so the caller can retry it. */
    public int process(List<ClickEvent> events) {
        if (events.isEmpty()) {
            return 0;
        }
        Map<String, GeoIpService.Location> locations = geoIpService.lookup(
                events.stream().map(ClickEvent::ip).filter(Objects::nonNull).toList());
        int written = writer.write(events, locations);
        stats.recordProcessed(written, events.size() - written);
        return written;
    }
}
