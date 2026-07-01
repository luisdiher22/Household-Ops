package com.householdops.app.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Default cache-aside config for all @Cacheable caches: JSON values
     * (readable directly in Redis, not JDK-serialized blobs) and a short
     * default TTL as a belt-and-suspenders freshness guard alongside the
     * explicit @CacheEvict calls on writes.
     *
     * Reuses Spring Boot's own autoconfigured ObjectMapper (JSR-310 module
     * registered for java.time types) rather than
     * GenericJackson2JsonRedisSerializer's no-arg constructor, which builds
     * a bare ObjectMapper with no modules and fails on Instant fields.
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(ObjectMapper objectMapper) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));

        return builder -> builder.cacheDefaults(defaultConfig);
    }
}
