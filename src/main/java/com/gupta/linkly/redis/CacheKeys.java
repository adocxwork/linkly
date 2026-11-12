package com.gupta.linkly.redis;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

/** Central place for Redis key names and TTLs. */
public final class CacheKeys {

    private CacheKeys() {
    }

    /**
     * Short TTLs bound how long a stale entry can live if an eviction was missed while Redis
     * was unreachable.
     */
    public static final Duration PROFILE_TTL = Duration.ofMinutes(10);
    public static final Duration REDIRECT_TTL = Duration.ofHours(1);

    public static String profile(String username) {
        return "profile:v2:" + username;
    }

    public static String redirect(String shortUrl) {
        return "redirect:v2:" + shortUrl;
    }

    public static List<String> redirects(Collection<String> shortUrls) {
        return shortUrls.stream().map(CacheKeys::redirect).toList();
    }
}
