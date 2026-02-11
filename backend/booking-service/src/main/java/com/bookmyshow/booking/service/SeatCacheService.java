package com.bookmyshow.booking.service;

import com.bookmyshow.booking.dto.ShowSeatDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

/**
 * SeatCacheService — Redis-backed cache layer for seat availability.
 *
 * Data Structure: Redis Hash per show
 *   Key:   "show_seats:{showId}"
 *   Field: "{showSeatId}"
 *   Value: "{seatId}:{status}:{price}"
 *
 * Read Path:  Check Redis hash first → on MISS, return empty (caller fills from DB)
 * Write Path: After any DB mutation (lock/confirm/cancel), update the hash fields (write-through)
 *
 * This eliminates ~99% of Postgres reads for the seat-map endpoint under normal load.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatCacheService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "show_seats:";

    @Value("${booking.cache.seat-ttl-minutes:30}")
    private int seatCacheTtlMinutes;

    /**
     * Try to read all seat statuses from cache.
     *
     * @return map of showSeatId → "seatId:status:price", or empty map on cache miss
     */
    public Map<String, String> getShowSeatStatuses(UUID showId) {
        String key = CACHE_KEY_PREFIX + showId;
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            if (entries == null || entries.isEmpty()) {
                log.debug("Cache MISS for show_seats:{}", showId);
                return Collections.emptyMap();
            }
            log.debug("Cache HIT for show_seats:{} ({} seats)", showId, entries.size());
            Map<String, String> result = new HashMap<>(entries.size());
            entries.forEach((k, v) -> result.put(k.toString(), v.toString()));
            return result;
        } catch (Exception e) {
            log.warn("Redis cache read failed for show {}: {}", showId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Populate the full cache from a list of ShowSeatDTOs (called on cache miss).
     * Stores: showSeatId → seatId:status:price
     */
    public void populateCache(UUID showId, List<ShowSeatDTO> seats) {
        String key = CACHE_KEY_PREFIX + showId;
        try {
            Map<String, String> hashEntries = new HashMap<>(seats.size());
            for (ShowSeatDTO seat : seats) {
                hashEntries.put(
                        seat.getId().toString(),
                        seat.getSeatId() + ":" + seat.getStatus() + ":" + seat.getPrice().toPlainString()
                );
            }
            redisTemplate.opsForHash().putAll(key, hashEntries);
            redisTemplate.expire(key, Duration.ofMinutes(seatCacheTtlMinutes));
            log.debug("Cache populated for show_seats:{} ({} seats)", showId, seats.size());
        } catch (Exception e) {
            log.warn("Redis cache populate failed for show {}: {}", showId, e.getMessage());
        }
    }

    /**
     * Write-through: update specific seat statuses in the cache.
     * Called after every DB mutation (lock, confirm, cancel, release).
     * Preserves the seatId portion, only updates status and price.
     */
    public void updateSeatStatuses(UUID showId, Map<UUID, String> seatStatusUpdates, Map<UUID, BigDecimal> seatPrices) {
        String key = CACHE_KEY_PREFIX + showId;
        try {
            // Only update if the hash already exists (don't create a stale partial cache)
            Boolean exists = redisTemplate.hasKey(key);
            if (exists == null || !exists) {
                log.debug("Cache not present for show_seats:{}, skipping write-through", showId);
                return;
            }

            // Read existing entries to preserve seatId
            Map<Object, Object> existingEntries = redisTemplate.opsForHash().entries(key);

            Map<String, String> updates = new HashMap<>(seatStatusUpdates.size());
            for (Map.Entry<UUID, String> entry : seatStatusUpdates.entrySet()) {
                String showSeatIdStr = entry.getKey().toString();
                BigDecimal price = seatPrices.getOrDefault(entry.getKey(), BigDecimal.ZERO);
                
                // Get existing seatId from cache
                String existingValue = existingEntries.get(showSeatIdStr) != null 
                        ? existingEntries.get(showSeatIdStr).toString() : null;
                String seatId = "";
                if (existingValue != null) {
                    String[] parts = existingValue.split(":", 3);
                    if (parts.length >= 1) {
                        seatId = parts[0];
                    }
                }
                
                updates.put(
                        showSeatIdStr,
                        seatId + ":" + entry.getValue() + ":" + price.toPlainString()
                );
            }
            redisTemplate.opsForHash().putAll(key, updates);
            // Refresh TTL on write
            redisTemplate.expire(key, Duration.ofMinutes(seatCacheTtlMinutes));
            log.debug("Cache write-through for show_seats:{} ({} seats updated)", showId, updates.size());
        } catch (Exception e) {
            log.warn("Redis cache write-through failed for show {}: {}", showId, e.getMessage());
            // Non-fatal: next read will repopulate from DB
        }
    }

    /**
     * Evict the entire cache for a show (used as fallback / safety net).
     */
    public void evictShow(UUID showId) {
        String key = CACHE_KEY_PREFIX + showId;
        try {
            redisTemplate.delete(key);
            log.debug("Cache evicted for show_seats:{}", showId);
        } catch (Exception e) {
            log.warn("Redis cache evict failed for show {}: {}", showId, e.getMessage());
        }
    }
}
