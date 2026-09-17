package com.gupta.linkly.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import java.time.Duration;

@Configuration
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public org.springframework.data.redis.stream.StreamMessageListenerContainer<String, org.springframework.data.redis.connection.stream.MapRecord<String, String, String>> streamMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            com.gupta.linkly.service.AnalyticsStreamConsumer streamConsumer) {

        org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, org.springframework.data.redis.connection.stream.MapRecord<String, String, String>> options =
                org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofMillis(100))
                        .build();

        org.springframework.data.redis.stream.StreamMessageListenerContainer<String, org.springframework.data.redis.connection.stream.MapRecord<String, String, String>> container =
                org.springframework.data.redis.stream.StreamMessageListenerContainer.create(connectionFactory, options);

        try {
            connectionFactory.getConnection().xGroupCreate("link-clicks-stream".getBytes(), "analytics-group", org.springframework.data.redis.connection.stream.ReadOffset.from("0-0"), true);
        } catch (Exception e) {
            // Group might already exist
        }

        container.receive(
                org.springframework.data.redis.connection.stream.Consumer.from("analytics-group", "consumer-1"),
                org.springframework.data.redis.connection.stream.StreamOffset.create("link-clicks-stream", org.springframework.data.redis.connection.stream.ReadOffset.lastConsumed()),
                streamConsumer);

        container.start();
        return container;
    }
}
