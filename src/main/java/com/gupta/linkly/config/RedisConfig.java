package com.gupta.linkly.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Builds the Redis connection ourselves instead of relying on spring.data.redis.url, because
 * Spring Boot refuses to START when that URL is empty, and Redis must be optional.
 *
 * <ul>
 *   <li>Empty REDIS_URL: a factory pointing at localhost is created but never used
 *       (RedisGateway sees "not configured").</li>
 *   <li>Connection is lazy: nothing connects at startup, so an unreachable Redis cannot block boot.</li>
 *   <li>REJECT_COMMANDS while disconnected: commands fail immediately instead of queueing until
 *       the timeout, so the fallback path kicks in fast.</li>
 * </ul>
 */
@Configuration
public class RedisConfig {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(@Value("${app.redis.url:}") String url) {
        RedisStandaloneConfiguration server = new RedisStandaloneConfiguration();
        boolean ssl = false;
        if (url != null && !url.isBlank()) {
            URI uri = URI.create(url.trim());
            ssl = "rediss".equalsIgnoreCase(uri.getScheme());
            server.setHostName(uri.getHost());
            server.setPort(uri.getPort() > 0 ? uri.getPort() : 6379);
            if (uri.getRawUserInfo() != null) {
                String[] parts = uri.getRawUserInfo().split(":", 2);
                if (parts.length == 2) {
                    if (!parts[0].isEmpty()) {
                        server.setUsername(decode(parts[0]));
                    }
                    server.setPassword(decode(parts[1]));
                } else {
                    server.setPassword(decode(parts[0]));
                }
            }
            String path = uri.getPath();
            if (path != null && path.length() > 1) {
                server.setDatabase(Integer.parseInt(path.substring(1)));
            }
        }

        ClientOptions clientOptions = ClientOptions.builder()
                .autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .socketOptions(SocketOptions.builder().connectTimeout(TIMEOUT).build())
                .timeoutOptions(TimeoutOptions.enabled(TIMEOUT))
                .build();
        LettuceClientConfiguration.LettuceClientConfigurationBuilder client = LettuceClientConfiguration.builder()
                .commandTimeout(TIMEOUT)
                .shutdownTimeout(Duration.ofMillis(200))
                .clientOptions(clientOptions);
        if (ssl) {
            client.useSsl();
        }
        return new LettuceConnectionFactory(server, client.build());
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
