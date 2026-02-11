package com.bookmyshow.movie.repository;

import com.bookmyshow.movie.model.Show;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShowRepository extends JpaRepository<Show, UUID> {

    @Query("SELECT s FROM Show s WHERE s.movie.id = :movieId AND s.showDate = :date " +
           "AND s.isActive = true ORDER BY s.startTime")
    List<Show> findByMovieIdAndShowDate(@Param("movieId") UUID movieId,
                                         @Param("date") LocalDate date);

    @Query("SELECT s FROM Show s JOIN s.screen sc JOIN sc.theater t " +
           "WHERE s.movie.id = :movieId AND t.city.id = :cityId " +
           "AND s.showDate = :date AND s.isActive = true " +
           "ORDER BY t.name, s.startTime")
    List<Show> findByMovieCityAndDate(@Param("movieId") UUID movieId,
                                       @Param("cityId") UUID cityId,
                                       @Param("date") LocalDate date);

    List<Show> findByScreenIdAndShowDateAndIsActiveTrue(UUID screenId, LocalDate date);
    
    List<Show> findByScreenIdAndShowDateOrderByStartTime(UUID screenId, LocalDate date);

    @Query("SELECT s FROM Show s WHERE s.showDate < :today AND s.isActive = true")
    List<Show> findPastActiveShows(@Param("today") LocalDate today);
    
    // Admin queries
    Page<Show> findByMovieId(UUID movieId, Pageable pageable);
    
    Page<Show> findByShowDate(LocalDate date, Pageable pageable);
    
    @Query("SELECT s FROM Show s WHERE s.movie.id = :movieId AND s.showDate = :date")
    Page<Show> findByMovieIdAndShowDate(@Param("movieId") UUID movieId, 
                                         @Param("date") LocalDate date, 
                                         Pageable pageable);
    
    @Query("SELECT s FROM Show s WHERE s.screen.id = :screenId AND s.showDate = :date")
    Page<Show> findByScreenIdAndShowDate(@Param("screenId") UUID screenId, 
                                          @Param("date") LocalDate date, 
                                          Pageable pageable);
    
    long countByShowDate(LocalDate date);
    
    @Query("SELECT s FROM Show s WHERE s.screen.id = :screenId AND s.showDate = :date " +
           "AND ((s.startTime <= :startTime AND s.endTime > :startTime) " +
           "OR (s.startTime < :endTime AND s.endTime >= :endTime) " +
           "OR (s.startTime >= :startTime AND s.endTime <= :endTime))")
    List<Show> findConflictingShows(@Param("screenId") UUID screenId,
                                     @Param("date") LocalDate date,
                                     @Param("startTime") LocalTime startTime,
                                     @Param("endTime") LocalTime endTime);
}
