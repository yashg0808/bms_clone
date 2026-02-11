package com.bookmyshow.movie.repository;

import com.bookmyshow.movie.model.ShowSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, UUID> {

    List<ShowSeat> findByShowId(UUID showId);
    
    long countByShowId(UUID showId);
    
    void deleteByShowId(UUID showId);
}
