package com.bookmyshow.booking.service;

import com.bookmyshow.booking.exception.SeatUnavailableException;
import com.bookmyshow.booking.model.SeatStatus;
import com.bookmyshow.booking.model.ShowSeat;
import com.bookmyshow.booking.repository.ShowSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatLockServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ShowSeatRepository showSeatRepository;

    @Mock
    private RLock rLock;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private SeatLockService seatLockService;

    private UUID showId;
    private List<UUID> seatIds;
    private List<ShowSeat> availableSeats;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(seatLockService, "seatLockTimeoutMinutes", 8);
        ReflectionTestUtils.setField(seatLockService, "distributedLockWaitSeconds", 5);
        ReflectionTestUtils.setField(seatLockService, "distributedLockLeaseSeconds", 10);

        showId = UUID.randomUUID();
        seatIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        availableSeats = seatIds.stream()
                .map(id -> {
                    ShowSeat seat = new ShowSeat();
                    seat.setId(id);
                    seat.setShowId(showId);
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seat.setPrice(BigDecimal.valueOf(250.00));
                    seat.setVersion(0);
                    return seat;
                })
                .toList();
    }

    @Test
    @DisplayName("Should lock seats successfully when all seats are available")
    void lockSeats_Success() throws InterruptedException {
        // Arrange
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(showSeatRepository.findByShowIdAndIdIn(showId, seatIds)).thenReturn(availableSeats);
        when(showSeatRepository.saveAll(anyList())).thenReturn(availableSeats);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        String lockToken = seatLockService.lockSeats(showId, seatIds);

        // Assert
        assertThat(lockToken).isNotNull().isNotEmpty();
        verify(showSeatRepository).saveAll(anyList());
        verify(valueOperations).set(anyString(), anyString(), any());
        verify(rLock).unlock();

        // Verify seats were updated to LOCKED
        for (ShowSeat seat : availableSeats) {
            assertThat(seat.getStatus()).isEqualTo(SeatStatus.LOCKED);
            assertThat(seat.getLockedAt()).isNotNull();
        }
    }

    @Test
    @DisplayName("Should throw SeatUnavailableException when seats are already locked")
    void lockSeats_SeatsAlreadyLocked() throws InterruptedException {
        // Arrange
        availableSeats.get(0).setStatus(SeatStatus.LOCKED);
        availableSeats.get(0).setLockedBy(UUID.randomUUID());

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(showSeatRepository.findByShowIdAndIdIn(showId, seatIds)).thenReturn(availableSeats);

        // Act & Assert
        assertThatThrownBy(() -> seatLockService.lockSeats(showId, seatIds))
                .isInstanceOf(SeatUnavailableException.class)
                .hasMessageContaining("no longer available");

        verify(showSeatRepository, never()).saveAll(anyList());
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("Should throw when distributed lock cannot be acquired (high contention)")
    void lockSeats_DistributedLockFailed() throws InterruptedException {
        // Arrange
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> seatLockService.lockSeats(showId, seatIds))
                .isInstanceOf(SeatUnavailableException.class)
                .hasMessageContaining("High demand");

        verify(showSeatRepository, never()).findByShowIdAndIdIn(any(), any());
    }

    @Test
    @DisplayName("Should throw when seat IDs don't match existing seats")
    void lockSeats_SeatNotFound() throws InterruptedException {
        // Arrange - return only 1 seat when 2 were requested
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(showSeatRepository.findByShowIdAndIdIn(showId, seatIds)).thenReturn(List.of(availableSeats.get(0)));

        // Act & Assert
        assertThatThrownBy(() -> seatLockService.lockSeats(showId, seatIds))
                .isInstanceOf(SeatUnavailableException.class)
                .hasMessageContaining("do not exist");
    }

    @Test
    @DisplayName("Should release seats successfully")
    void releaseSeats_Success() {
        // Arrange
        String lockToken = UUID.randomUUID().toString();
        String tokenKey = "seat:lock:token:" + lockToken;
        String tokenValue = showId + "|" + seatIds.get(0) + "," + seatIds.get(1);

        // Set seats to LOCKED state
        availableSeats.forEach(s -> {
            s.setStatus(SeatStatus.LOCKED);
        });

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(tokenKey)).thenReturn(tokenValue);
        when(showSeatRepository.findByShowIdAndIdIn(showId, seatIds)).thenReturn(availableSeats);
        when(showSeatRepository.saveAll(anyList())).thenReturn(availableSeats);
        when(redisTemplate.delete(tokenKey)).thenReturn(true);

        // Act
        seatLockService.releaseSeats(lockToken);

        // Assert
        for (ShowSeat seat : availableSeats) {
            assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
            assertThat(seat.getLockedBy()).isNull();
        }
        verify(redisTemplate).delete(tokenKey);
    }

    @Test
    @DisplayName("Should validate lock token correctly")
    void validateLockToken_Success() {
        // Arrange
        String lockToken = UUID.randomUUID().toString();
        String tokenKey = "seat:lock:token:" + lockToken;
        String tokenValue = showId + "|" + seatIds.get(0) + "," + seatIds.get(1);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(tokenKey)).thenReturn(tokenValue);

        // Act
        List<UUID> result = seatLockService.validateLockToken(lockToken);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrderElementsOf(seatIds);
    }

    @Test
    @DisplayName("Should return null for expired or invalid lock token")
    void validateLockToken_Expired() {
        // Arrange
        String lockToken = UUID.randomUUID().toString();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // Act
        List<UUID> result = seatLockService.validateLockToken(lockToken);

        // Assert
        assertThat(result).isNull();
    }
}
