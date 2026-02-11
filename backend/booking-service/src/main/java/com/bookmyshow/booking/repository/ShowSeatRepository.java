package com.bookmyshow.booking.repository;

import com.bookmyshow.booking.dto.ShowSeatDTO;
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

    /**
     * Efficient projection query - fetches only id and status for availability checks.
     * Avoids loading full ShowSeat entity until we confirm seats are available.
     */
    @Query("SELECT ss.id as id, ss.status as status FROM ShowSeat ss WHERE ss.showId = :showId AND ss.id IN :seatIds")
    List<SeatStatusProjection> findSeatStatusesByShowIdAndIdIn(
            @Param("showId") UUID showId,
            @Param("seatIds") List<UUID> seatIds
    );

    @Query("SELECT ss FROM ShowSeat ss WHERE ss.status = 'LOCKED' AND ss.lockedAt < :expiry")
    List<ShowSeat> findExpiredLocks(@Param("expiry") LocalDateTime expiry);

    @Modifying
    @Query("UPDATE ShowSeat ss SET ss.status = 'AVAILABLE', ss.lockedBy = null, ss.lockedAt = null " +
            "WHERE ss.status = 'LOCKED' AND ss.lockedAt < :expiry")
    int releaseExpiredLocks(@Param("expiry") LocalDateTime expiry);

    @Query("SELECT COUNT(ss) FROM ShowSeat ss WHERE ss.showId = :showId AND ss.status = :status")
    long countByShowIdAndStatus(@Param("showId") UUID showId, @Param("status") SeatStatus status);

    /**
     * Fetch show seats joined with seat layout info (row, number, type, column).
     */
    @Query(nativeQuery = true, value =
            "SELECT ss.id, ss.show_id AS showId, ss.seat_id AS seatId, " +
            "CAST(ss.status AS TEXT) AS status, ss.price, " +
            "s.row_name AS seatRow, s.seat_number AS seatNumber, " +
            "CAST(s.seat_type AS TEXT) AS seatType, s.column_number AS columnNumber " +
            "FROM show_seats ss " +
            "JOIN seats s ON s.id = ss.seat_id " +
            "WHERE ss.show_id = :showId " +
            "ORDER BY s.row_name, s.column_number")
    List<Object[]> findShowSeatsWithSeatInfo(@Param("showId") UUID showId);

    /**
     * Fetch seat info for specific show_seat IDs (used during lock/booking).
     */
    @Query(nativeQuery = true, value =
            "SELECT ss.id, ss.show_id AS showId, ss.seat_id AS seatId, " +
            "CAST(ss.status AS TEXT) AS status, ss.price, " +
            "s.row_name AS seatRow, s.seat_number AS seatNumber, " +
            "CAST(s.seat_type AS TEXT) AS seatType, s.column_number AS columnNumber " +
            "FROM show_seats ss " +
            "JOIN seats s ON s.id = ss.seat_id " +
            "WHERE ss.id IN :seatIds " +
            "ORDER BY s.row_name, s.column_number")
    List<Object[]> findSeatInfoByShowSeatIds(@Param("seatIds") List<UUID> seatIds);
}
