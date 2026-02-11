package com.bookmyshow.movie.repository;

import com.bookmyshow.movie.model.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID> {

    Page<Movie> findByIsActiveTrue(Pageable pageable);
    
    long countByIsActiveTrue();

    @Query("SELECT DISTINCT m FROM Movie m JOIN Show s ON s.movie = m " +
           "JOIN Screen sc ON s.screen = sc " +
           "JOIN Theater t ON sc.theater = t " +
           "WHERE t.city.id = :cityId AND m.isActive = true AND s.showDate >= :today")
    Page<Movie> findMoviesByCity(@Param("cityId") UUID cityId,
                                 @Param("today") LocalDate today,
                                 Pageable pageable);

    Page<Movie> findByLanguageAndIsActiveTrue(String language, Pageable pageable);

    Page<Movie> findByGenreContainingIgnoreCaseAndIsActiveTrue(String genre, Pageable pageable);

    @Query("SELECT m FROM Movie m WHERE m.isActive = true AND m.releaseDate <= :today " +
           "ORDER BY m.imdbRating DESC NULLS LAST")
    List<Movie> findFeaturedMovies(@Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT m FROM Movie m WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "AND m.isActive = true")
    Page<Movie> searchByTitle(@Param("query") String query, Pageable pageable);
}
