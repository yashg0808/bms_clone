package com.bookmyshow.movie.controller;

import com.bookmyshow.movie.dto.admin.DashboardStats;
import com.bookmyshow.movie.service.AdminDashboardService;
import com.bookmyshow.movie.service.CacheManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * Admin controller for dashboard and system management.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;
    private final CacheManagementService cacheManagementService;

    // ==================== Dashboard ====================

    /**
     * Get dashboard statistics.
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStats> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    // ==================== Cache Management ====================

    /**
     * Get all cache names.
     */
    @GetMapping("/cache")
    public ResponseEntity<Collection<String>> getCacheNames() {
        return ResponseEntity.ok(cacheManagementService.getCacheNames());
    }

    /**
     * Get cache status.
     */
    @GetMapping("/cache/status")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        return ResponseEntity.ok(cacheManagementService.getCacheStatus());
    }

    /**
     * Clear a specific cache.
     */
    @DeleteMapping("/cache/{cacheName}")
    public ResponseEntity<Map<String, String>> clearCache(@PathVariable String cacheName) {
        cacheManagementService.clearCache(cacheName);
        return ResponseEntity.ok(Map.of("message", "Cache '" + cacheName + "' cleared successfully"));
    }

    /**
     * Clear all caches.
     */
    @DeleteMapping("/cache")
    public ResponseEntity<Map<String, String>> clearAllCaches() {
        cacheManagementService.clearAllCaches();
        return ResponseEntity.ok(Map.of("message", "All caches cleared successfully"));
    }

    /**
     * Evict a specific key from a cache.
     */
    @DeleteMapping("/cache/{cacheName}/key/{key}")
    public ResponseEntity<Map<String, String>> evictFromCache(
            @PathVariable String cacheName,
            @PathVariable String key) {
        cacheManagementService.evictFromCache(cacheName, key);
        return ResponseEntity.ok(Map.of("message", "Key '" + key + "' evicted from cache '" + cacheName + "'"));
    }
}
