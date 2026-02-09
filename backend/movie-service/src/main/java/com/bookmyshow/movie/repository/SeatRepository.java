package com.bookmyshow.movie.repository;

import com.bookmyshow.movie.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SeatRepository extends JpaRepository<Seat, UUID> {

    List<Seat> findByScreenIdAndIsActiveTrueOrderByRowNameAscColumnNumberAsc(UUID screenId);
}
