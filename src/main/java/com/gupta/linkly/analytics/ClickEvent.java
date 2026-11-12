package com.gupta.linkly.analytics;

import java.util.UUID;

/**
 * A single click on a short link, as published to the queue (Redis Stream or the in-memory
 * fallback). It carries the raw IP only so the worker can do the Geo-IP lookup; the IP is hashed
 * before anything is written to the database.
 */
public record ClickEvent(UUID linkId, String ip, String userAgent, long occurredAtMillis) {
}
