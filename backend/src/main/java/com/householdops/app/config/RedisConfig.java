package com.householdops.app.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Default cache-aside config for all @Cacheable caches: JSON values
     * (readable directly in Redis, not JDK-serialized blobs) and a short
     * default TTL as a belt-and-suspenders freshness guard alongside the
     * explicit @CacheEvict calls on writes.
     *
     * Uses a dedicated ObjectMapper, not Spring Boot's shared bean or
     * GenericJackson2JsonRedisSerializer's own no-arg default: the shared
     * bean has JSR-310 support (java.time types) but no polymorphic type
     * info, so a cache *hit* deserializes back as a raw LinkedHashMap
     * instead of the actual DTO record -- a ClassCastException surfaced
     * this the first time a cached value was actually read back rather
     * than just written. activateDefaultTyping embeds a "@class" hint per
     * value (only in Redis, never in the app's normal REST responses,
     * since this mapper isn't the one used for HTTP serialization).
     *
     * DefaultTyping.EVERYTHING, not NON_FINAL: the cached DTOs are Java
     * records, which are implicitly final, so NON_FINAL silently skips
     * embedding their type id. EVERYTHING tags every non-primitive field
     * (including plain JDK types like UUID, not just our own DTOs), so a
     * validator scoped to our own package rejects those -- unrestricted
     * (LaissezFaireSubTypeValidator) is the correct choice here specifically
     * because this mapper only ever reads back what this same app wrote to
     * its own Redis instance, never untrusted external input.
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        ObjectMapper redisObjectMapper = new ObjectMapper();
        redisObjectMapper.registerModule(new JavaTimeModule());
        redisObjectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper)));

        return builder -> builder.cacheDefaults(defaultConfig);
    }
}
