package com.bookmyshow.movie.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Service for cache management operations.
 */
@Service
@RequiredArgsConstructor
public class CacheManagementService {

    private static final Logger log = LoggerFactory.getLogger(CacheManagementService.class);

    private final CacheManager cacheManager;

    /**
     * Get all cache names.
     */
    public Collection<String> getCacheNames() {
        return cacheManager.getCacheNames();
    }

    /**
     * Clear a specific cache.
     */
    public void clearCache(String cacheName) {
        log.info("Clearing cache: {}", cacheName);
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("Cache cleared: {}", cacheName);
        } else {
            log.warn("Cache not found: {}", cacheName);
        }
    }

    /**
     * Clear all caches.
     */
    public void clearAllCaches() {
        log.warn("Clearing ALL caches");
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
        log.info("All caches cleared");
    }

    /**
     * Evict a specific key from a cache.
     */
    public void evictFromCache(String cacheName, String key) {
        log.info("Evicting key {} from cache {}", key, cacheName);
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.info("Key evicted from cache");
        } else {
            log.warn("Cache not found: {}", cacheName);
        }
    }

    /**
     * Get cache status (names only - Spring Cache doesn't expose size).
     */
    public Map<String, Object> getCacheStatus() {
        Map<String, Object> status = new HashMap<>();
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            status.put(name, Map.of(
                "exists", cache != null,
                "type", cache != null ? cache.getClass().getSimpleName() : "N/A"
            ));
        });
        return status;
    }
}
