package com.gupta.linkly.config;

import com.gupta.linkly.redis.RedisEndpoint;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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

    @Bean
    public ThreadPoolTaskExecutor clickPublishExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("click-publish-");
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1_000);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        return executor;
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(RedisEndpoint endpoint,
                                                           @Value("${app.redis.timeout:5s}") Duration timeout) {
        ClientOptions clientOptions = ClientOptions.builder()
                // Start connections with AUTH/PING using the Redis-compatible RESP2 handshake.
                .protocolVersion(ProtocolVersion.RESP2)
                .autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .socketOptions(SocketOptions.builder().connectTimeout(timeout).build())
                .timeoutOptions(TimeoutOptions.enabled(timeout))
                .build();
        LettuceClientConfiguration.LettuceClientConfigurationBuilder client = LettuceClientConfiguration.builder()
                .commandTimeout(timeout)
                .shutdownTimeout(Duration.ofMillis(200))
                .clientOptions(clientOptions);
        if (endpoint.tls()) {
            client.useSsl();
        }
        return new LettuceConnectionFactory(endpoint.server(), client.build());
    }
}
