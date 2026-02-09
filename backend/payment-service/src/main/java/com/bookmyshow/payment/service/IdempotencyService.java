package com.bookmyshow.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * IdempotencyService - Ensures payment operations are idempotent.
 * 
 * Uses Redis to store idempotency keys with TTL.
 * If a request with the same idempotency key is received within the TTL,
 * the original result is returned instead of processing again.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String IDEMPOTENCY_PREFIX = "payment:idempotency:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    /**
     * Check if an idempotency key already exists.
     *
     * @param idempotencyKey the unique key
     * @return true if the key already exists (duplicate request)
     */
    public boolean isDuplicate(String idempotencyKey) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Store the idempotency key with the payment ID result.
     *
     * @param idempotencyKey the unique key
     * @param paymentId      the payment ID result
     */
    public void storeResult(String idempotencyKey, String paymentId) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        redisTemplate.opsForValue().set(key, paymentId, IDEMPOTENCY_TTL);
        log.debug("Stored idempotency key: {} -> {}", idempotencyKey, paymentId);
    }

    /**
     * Get the stored result for an idempotency key.
     *
     * @param idempotencyKey the unique key
     * @return the payment ID, or null if not found
     */
    public String getStoredResult(String idempotencyKey) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        return redisTemplate.opsForValue().get(key);
    }
}
