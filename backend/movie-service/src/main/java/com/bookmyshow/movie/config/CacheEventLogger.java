package com.bookmyshow.movie.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Logs cache operations for monitoring and debugging.
 * 
 * Cache hit/miss logging strategy:
 * - MISS: Logged in service methods (since @Cacheable only executes method body on miss)
 * - HIT: Not logged directly (would require AOP), but absence of MISS log = cache hit
 * 
 * For production, consider using Micrometer metrics instead of logs.
 */
@Component
public class CacheEventLogger {

    private static final Logger log = LoggerFactory.getLogger("CacheLogger");

    /**
     * Log a cache miss event. Call this at the start of @Cacheable methods.
     * If this log appears, the cache was missed and DB is being queried.
     */
    public static void logCacheMiss(String cacheName, Object... keyParts) {
        if (log.isDebugEnabled()) {
            String key = Arrays.stream(keyParts)
                    .map(Object::toString)
                    .reduce((a, b) -> a + ":" + b)
                    .orElse("unknown");
            log.debug("CACHE MISS [{}] key={}", cacheName, key);
        }
    }

    /**
     * Log cache population after fetching from DB.
     */
    public static void logCachePopulate(String cacheName, Object key, int itemCount) {
        if (log.isDebugEnabled()) {
            log.debug("CACHE POPULATE [{}] key={} items={}", cacheName, key, itemCount);
        }
    }

    /**
     * Log cache hit (for manual cache lookups, not @Cacheable).
     */
    public static void logCacheHit(String cacheName, Object key) {
        if (log.isDebugEnabled()) {
            log.debug("CACHE HIT [{}] key={}", cacheName, key);
        }
    }
}
