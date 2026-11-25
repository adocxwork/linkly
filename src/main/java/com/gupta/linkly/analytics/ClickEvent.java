package com.gupta.linkly.analytics;

import java.util.UUID;

/** Queued click. A stable ID follows the event across Redis and the fallback path. */
public record ClickEvent(UUID eventId, UUID linkId, String ip, String userAgent, Long occurredAtMillis) {
}
