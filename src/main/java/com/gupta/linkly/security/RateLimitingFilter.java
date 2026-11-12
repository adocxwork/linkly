package com.gupta.linkly.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.gupta.linkly.web.ClientIpResolver;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Per-IP token-bucket rate limiting (Bucket4j) for the endpoints anonymous users can hit.
 *
 * <p>Buckets live in a bounded Caffeine cache that forgets idle IPs after 10 minutes. The old
 * ConcurrentHashMap never removed entries, so every distinct IP leaked memory forever.
 *
 * <p>Trade-off: limits are per instance (in memory). Fine for a single Render instance; a
 * multi-instance deployment would move the buckets to Redis.
 */
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    enum Rule {
        AUTH(10), MESSAGE(5), PUBLIC_READ(60), REDIRECT(120);

        final int perMinute;

        Rule(int perMinute) {
            this.perMinute = perMinute;
        }
    }

    private final ClientIpResolver clientIpResolver;
    private final SecurityErrorWriter errorWriter;

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Rule rule = ruleFor(request);
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }
        String key = rule.name() + ":" + clientIpResolver.resolve(request);
        Bucket bucket = buckets.get(key, k -> newBucket(rule));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }
        long retryAfter = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        errorWriter.write(request, response, 429, "Too many requests. Please try again in " + retryAfter + " seconds.");
    }

    static Rule ruleFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("OPTIONS".equals(method)) {
            return null;
        }
        if (path.startsWith("/api/auth/")) {
            return Rule.AUTH;
        }
        if (path.startsWith("/api/public/")) {
            return "POST".equals(method) ? Rule.MESSAGE : Rule.PUBLIC_READ;
        }
        if (path.startsWith("/r/")) {
            return Rule.REDIRECT;
        }
        return null;
    }

    private static Bucket newBucket(Rule rule) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(rule.perMinute).refillGreedy(rule.perMinute, Duration.ofMinutes(1)).build())
                .build();
    }
}
