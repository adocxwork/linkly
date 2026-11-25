package com.gupta.linkly.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Parses the optional Redis endpoint without logging credentials or blocking application startup. */
@Component
public final class RedisEndpoint {

    private final boolean configured;
    private final RedisStandaloneConfiguration server;
    private final boolean tls;
    private final String error;

    public RedisEndpoint(@Value("${app.redis.url:}") String url) {
        configured = url != null && !url.isBlank();
        RedisStandaloneConfiguration parsed = new RedisStandaloneConfiguration();
        boolean secure = false;
        String problem = null;
        if (configured) {
            try {
                URI uri = new URI(url.strip());
                String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
                if (!scheme.equals("redis") && !scheme.equals("rediss")) {
                    throw new IllegalArgumentException("Use a Redis TCP URL (redis:// or rediss://), not a REST endpoint or CLI command.");
                }
                if (uri.getHost() == null || uri.getPort() == 0 || uri.getPort() > 65535
                        || uri.getQuery() != null || uri.getFragment() != null) {
                    throw new IllegalArgumentException("REDIS_URL needs a valid host and port without query parameters or a fragment.");
                }

                String userInfo = uri.getRawUserInfo();
                String[] credentials = userInfo == null ? new String[0] : userInfo.split(":", 2);
                boolean upstash = uri.getHost().toLowerCase(Locale.ROOT).endsWith(".upstash.io");
                if (upstash && (credentials.length == 0 || credentials[credentials.length - 1].isEmpty())) {
                    throw new IllegalArgumentException("Copy the full connection string from Upstash → Connect → TCP.");
                }
                // Upstash requires TLS even if an older deployment stored the single-s scheme.
                secure = scheme.equals("rediss") || upstash;
                parsed.setHostName(uri.getHost());
                parsed.setPort(uri.getPort() == -1 ? 6379 : uri.getPort());

                if (credentials.length > 0) {
                    if (credentials.length == 2 && !credentials[0].isEmpty()) {
                        parsed.setUsername(decode(credentials[0]));
                    }
                    parsed.setPassword(decode(credentials[credentials.length - 1]));
                }
                String path = uri.getPath();
                if (path != null && path.length() > 1) {
                    if (!path.matches("/[0-9]{1,9}")) {
                        throw new IllegalArgumentException("The optional Redis URL path must be a database number, such as /0.");
                    }
                    parsed.setDatabase(Integer.parseInt(path.substring(1)));
                }
            } catch (URISyntaxException e) {
                problem = "REDIS_URL is malformed. Paste the Redis TCP connection string without quotes.";
            } catch (IllegalArgumentException e) {
                problem = e.getMessage();
            }
        }
        server = problem == null ? parsed : new RedisStandaloneConfiguration();
        tls = problem == null && secure;
        error = problem;
    }

    private static String decode(String value) {
        // URI user-info can contain literal '+' characters. Form decoding treats them as spaces.
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    public boolean configured() {
        return configured;
    }

    public RedisStandaloneConfiguration server() {
        return server;
    }

    public boolean tls() {
        return tls;
    }

    public String error() {
        return error;
    }
}
