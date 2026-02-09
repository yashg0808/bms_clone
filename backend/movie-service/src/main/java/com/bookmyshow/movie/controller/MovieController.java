package com.bookmyshow.movie.controller;

import com.bookmyshow.movie.dto.MovieResponse;
import com.bookmyshow.movie.dto.ShowResponse;
import com.bookmyshow.movie.service.MovieService;
import com.bookmyshow.movie.service.ShowService;
import com.bookmyshow.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for movie catalog operations.
 */
@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final ShowService showService;

    /**
     * Get all active movies with pagination.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<MovieResponse>>> getMovies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(movieService.getMovies(page, size)));
    }

    /**
     * Get a specific movie by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> getMovieById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(movieService.getMovieById(id)));
    }

    /**
     * Get movies playing in a specific city.
     */
    @GetMapping("/city/{cityId}")
    public ResponseEntity<ApiResponse<Page<MovieResponse>>> getMoviesByCity(
            @PathVariable UUID cityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(movieService.getMoviesByCity(cityId, page, size)));
    }

    /**
     * Search movies by title.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<MovieResponse>>> searchMovies(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(movieService.searchMovies(q, page, size)));
    }

    /**
     * Get featured/trending movies.
     */
    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getFeaturedMovies() {
        return ResponseEntity.ok(ApiResponse.success(movieService.getFeaturedMovies()));
    }

    /**
     * Get shows for a movie on a specific date.
     */
    @GetMapping("/{id}/shows")
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getShowsByMovie(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID cityId) {
        List<ShowResponse> shows;
        if (cityId != null) {
            shows = showService.getShowsByMovieCityAndDate(id, cityId, date);
        } else {
            shows = showService.getShowsByMovieAndDate(id, date);
        }
        return ResponseEntity.ok(ApiResponse.success(shows));
    }
}
