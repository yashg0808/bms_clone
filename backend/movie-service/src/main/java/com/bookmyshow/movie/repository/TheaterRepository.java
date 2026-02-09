package com.bookmyshow.movie.repository;

import com.bookmyshow.movie.model.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TheaterRepository extends JpaRepository<Theater, UUID> {

    List<Theater> findByCityIdAndIsActiveTrue(UUID cityId);

    @Query("SELECT DISTINCT t FROM Theater t " +
           "JOIN Screen sc ON sc.theater = t " +
           "JOIN Show s ON s.screen = sc " +
           "WHERE s.movie.id = :movieId AND t.city.id = :cityId " +
           "AND s.showDate = :date AND s.isActive = true AND t.isActive = true")
    List<Theater> findTheatersByMovieAndCityAndDate(
            @Param("movieId") UUID movieId,
            @Param("cityId") UUID cityId,
            @Param("date") LocalDate date);
}
