package com.bookmyshow.booking.repository;

import com.bookmyshow.booking.model.ShowSeat;
import com.bookmyshow.booking.model.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, UUID> {

    List<ShowSeat> findByShowId(UUID showId);

    List<ShowSeat> findByShowIdAndStatus(UUID showId, SeatStatus status);

    List<ShowSeat> findByShowIdAndIdIn(UUID showId, List<UUID> seatIds);

    @Query("SELECT ss FROM ShowSeat ss WHERE ss.showId = :showId AND ss.id IN :seatIds AND ss.status = :status")
    List<ShowSeat> findByShowIdAndIdInAndStatus(
            @Param("showId") UUID showId,
            @Param("seatIds") List<UUID> seatIds,
            @Param("status") SeatStatus status
    );

    @Query("SELECT ss FROM ShowSeat ss WHERE ss.status = 'LOCKED' AND ss.lockedAt < :expiry")
    List<ShowSeat> findExpiredLocks(@Param("expiry") LocalDateTime expiry);

    @Modifying
    @Query("UPDATE ShowSeat ss SET ss.status = 'AVAILABLE', ss.lockedBy = null, ss.lockedAt = null " +
            "WHERE ss.status = 'LOCKED' AND ss.lockedAt < :expiry")
    int releaseExpiredLocks(@Param("expiry") LocalDateTime expiry);

    @Query("SELECT COUNT(ss) FROM ShowSeat ss WHERE ss.showId = :showId AND ss.status = :status")
    long countByShowIdAndStatus(@Param("showId") UUID showId, @Param("status") SeatStatus status);
}
