package com.bookmyshow.movie.repository;

import com.bookmyshow.movie.model.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, UUID> {

    List<Screen> findByTheaterIdAndIsActiveTrue(UUID theaterId);
    
    List<Screen> findByTheaterId(UUID theaterId);
}
