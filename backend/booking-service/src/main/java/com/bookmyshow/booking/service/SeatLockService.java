package com.bookmyshow.booking.service;

import com.bookmyshow.booking.exception.SeatUnavailableException;
import com.bookmyshow.booking.model.SeatStatus;
import com.bookmyshow.booking.model.ShowSeat;
import com.bookmyshow.booking.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * SeatLockService - Handles distributed seat locking using Redisson distributed locks.
 *
 * Flow:
 * 1. Acquire a Redisson distributed lock on the show (prevents concurrent lock attempts)
 * 2. Verify all requested seats are AVAILABLE
 * 3. Update seat status to LOCKED in DB with optimistic locking
 * 4. Store lock token in Redis with TTL
 * 5. Release distributed lock
 *
 * This ensures that even across multiple service instances, only one user
 * can lock a specific set of seats at a time.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatLockService {

    private final RedissonClient redissonClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ShowSeatRepository showSeatRepository;

    private static final String LOCK_KEY_PREFIX = "show:lock:";
    private static final String SEAT_LOCK_TOKEN_PREFIX = "seat:lock:token:";

    @Value("${booking.seat-lock.timeout-minutes:8}")
    private int seatLockTimeoutMinutes;

    @Value("${booking.seat-lock.distributed-lock-wait-seconds:5}")
    private int distributedLockWaitSeconds;

    @Value("${booking.seat-lock.distributed-lock-lease-seconds:10}")
    private int distributedLockLeaseSeconds;

    /**
     * Lock seats for a user. Uses Redisson distributed lock to prevent race conditions.
     *
     * @param showId  the show ID
     * @param seatIds the seat IDs to lock
     * @param userId  the user requesting the lock
     * @return lock token for subsequent operations
     */
    @Transactional
    public String lockSeats(UUID showId, List<UUID> seatIds, UUID userId) {
        String lockKey = LOCK_KEY_PREFIX + showId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Try to acquire distributed lock with timeout
            boolean acquired = lock.tryLock(distributedLockWaitSeconds, distributedLockLeaseSeconds, TimeUnit.SECONDS);
            if (!acquired) {
                throw new SeatUnavailableException("Unable to process seat selection. High demand detected. Please try again.");
            }

            try {
                // Fetch requested seats and verify they are all available
                List<ShowSeat> requestedSeats = showSeatRepository.findByShowIdAndIdIn(showId, seatIds);

                if (requestedSeats.size() != seatIds.size()) {
                    throw new SeatUnavailableException("One or more selected seats do not exist for this show.");
                }

                List<UUID> unavailableSeats = new ArrayList<>();
                for (ShowSeat seat : requestedSeats) {
                    if (seat.getStatus() != SeatStatus.AVAILABLE) {
                        unavailableSeats.add(seat.getId());
                    }
                }

                if (!unavailableSeats.isEmpty()) {
                    throw new SeatUnavailableException(
                            "Some selected seats are no longer available.",
                            unavailableSeats
                    );
                }

                // Generate a unique lock token
                String lockToken = UUID.randomUUID().toString();
                LocalDateTime lockedAt = LocalDateTime.now();
                LocalDateTime expiresAt = lockedAt.plusMinutes(seatLockTimeoutMinutes);

                // Update seat status to LOCKED in database
                for (ShowSeat seat : requestedSeats) {
                    seat.setStatus(SeatStatus.LOCKED);
                    seat.setLockedBy(userId);
                    seat.setLockedAt(lockedAt);
                }
                showSeatRepository.saveAll(requestedSeats);

                // Store lock token in Redis with TTL for validation
                String tokenKey = SEAT_LOCK_TOKEN_PREFIX + lockToken;
                StringBuilder seatIdsStr = new StringBuilder();
                for (UUID seatId : seatIds) {
                    if (seatIdsStr.length() > 0) seatIdsStr.append(",");
                    seatIdsStr.append(seatId);
                }
                String tokenValue = showId + "|" + userId + "|" + seatIdsStr;
                redisTemplate.opsForValue().set(tokenKey, tokenValue, Duration.ofMinutes(seatLockTimeoutMinutes));

                log.info("Seats locked successfully - showId: {}, userId: {}, seatCount: {}, lockToken: {}",
                        showId, userId, seatIds.size(), lockToken);

                return lockToken;

            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SeatUnavailableException("Seat locking was interrupted. Please try again.");
        }
    }

    /**
     * Release previously locked seats.
     *
     * @param lockToken the lock token obtained during locking
     * @param userId    the user requesting release
     */
    @Transactional
    public void releaseSeats(String lockToken, UUID userId) {
        String tokenKey = SEAT_LOCK_TOKEN_PREFIX + lockToken;
        String tokenValue = redisTemplate.opsForValue().get(tokenKey);

        if (tokenValue == null) {
            log.warn("Lock token not found or expired: {}", lockToken);
            return;
        }

        String[] parts = tokenValue.split("\\|");
        UUID showId = UUID.fromString(parts[0]);
        UUID lockUserId = UUID.fromString(parts[1]);

        if (!lockUserId.equals(userId)) {
            log.warn("User {} attempted to release lock owned by {}", userId, lockUserId);
            return;
        }

        String[] seatIdStrs = parts[2].split(",");
        List<UUID> seatIds = new ArrayList<>();
        for (String s : seatIdStrs) {
            seatIds.add(UUID.fromString(s.trim()));
        }

        // Release seats in database
        List<ShowSeat> seats = showSeatRepository.findByShowIdAndIdIn(showId, seatIds);
        for (ShowSeat seat : seats) {
            if (seat.getStatus() == SeatStatus.LOCKED && userId.equals(seat.getLockedBy())) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setLockedBy(null);
                seat.setLockedAt(null);
            }
        }
        showSeatRepository.saveAll(seats);

        // Remove lock token from Redis
        redisTemplate.delete(tokenKey);

        log.info("Seats released - showId: {}, userId: {}, seatCount: {}", showId, userId, seatIds.size());
    }

    /**
     * Validate a lock token and return the associated seat IDs.
     *
     * @param lockToken the lock token to validate
     * @param userId    the user ID to verify ownership
     * @return list of seat IDs associated with the lock
     */
    public List<UUID> validateLockToken(String lockToken, UUID userId) {
        String tokenKey = SEAT_LOCK_TOKEN_PREFIX + lockToken;
        String tokenValue = redisTemplate.opsForValue().get(tokenKey);

        if (tokenValue == null) {
            return null;
        }

        String[] parts = tokenValue.split("\\|");
        UUID lockUserId = UUID.fromString(parts[1]);

        if (!lockUserId.equals(userId)) {
            return null;
        }

        String[] seatIdStrs = parts[2].split(",");
        List<UUID> seatIds = new ArrayList<>();
        for (String s : seatIdStrs) {
            seatIds.add(UUID.fromString(s.trim()));
        }

        return seatIds;
    }

    /**
     * Get the show ID from a lock token.
     */
    public UUID getShowIdFromToken(String lockToken) {
        String tokenKey = SEAT_LOCK_TOKEN_PREFIX + lockToken;
        String tokenValue = redisTemplate.opsForValue().get(tokenKey);

        if (tokenValue == null) {
            return null;
        }

        String[] parts = tokenValue.split("\\|");
        return UUID.fromString(parts[0]);
    }

    /**
     * Delete the lock token from Redis after booking confirmation.
     */
    public void deleteLockToken(String lockToken) {
        String tokenKey = SEAT_LOCK_TOKEN_PREFIX + lockToken;
        redisTemplate.delete(tokenKey);
    }
}
