package com.bookmyshow.movie.controller;

import com.bookmyshow.movie.dto.MovieResponse;
import com.bookmyshow.movie.dto.PagedResponse;
import com.bookmyshow.movie.dto.admin.CreateMovieRequest;
import com.bookmyshow.movie.dto.admin.UpdateMovieRequest;
import com.bookmyshow.movie.service.AdminMovieService;
import com.bookmyshow.movie.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Admin controller for movie management.
 * All endpoints require ADMIN role (handled by security config).
 */
@RestController
@RequestMapping("/api/admin/movies")
@RequiredArgsConstructor
public class AdminMovieController {

    private final AdminMovieService adminMovieService;
    private final MovieService movieService;

    /**
     * Get all movies with pagination (including inactive).
     */
    @GetMapping
    public ResponseEntity<PagedResponse<MovieResponse>> getAllMovies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(adminMovieService.getAllMovies(page, size, sortBy, sortDir));
    }

    /**
     * Get a specific movie by ID.
     */
    @GetMapping("/{movieId}")
    public ResponseEntity<MovieResponse> getMovie(@PathVariable UUID movieId) {
        return ResponseEntity.ok(movieService.getMovieById(movieId));
    }

    /**
     * Create a new movie.
     */
    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(@Valid @RequestBody CreateMovieRequest request) {
        MovieResponse movie = adminMovieService.createMovie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(movie);
    }

    /**
     * Update an existing movie.
     */
    @PutMapping("/{movieId}")
    public ResponseEntity<MovieResponse> updateMovie(
            @PathVariable UUID movieId,
            @RequestBody UpdateMovieRequest request) {
        return ResponseEntity.ok(adminMovieService.updateMovie(movieId, request));
    }

    /**
     * Soft delete a movie (set isActive = false).
     */
    @DeleteMapping("/{movieId}")
    public ResponseEntity<Map<String, String>> deleteMovie(@PathVariable UUID movieId) {
        adminMovieService.deleteMovie(movieId);
        return ResponseEntity.ok(Map.of("message", "Movie deactivated successfully"));
    }

    /**
     * Hard delete a movie (permanent - use with caution).
     */
    @DeleteMapping("/{movieId}/permanent")
    public ResponseEntity<Map<String, String>> hardDeleteMovie(@PathVariable UUID movieId) {
        adminMovieService.hardDeleteMovie(movieId);
        return ResponseEntity.ok(Map.of("message", "Movie permanently deleted"));
    }

    /**
     * Toggle movie active status.
     */
    @PatchMapping("/{movieId}/toggle-active")
    public ResponseEntity<MovieResponse> toggleActive(@PathVariable UUID movieId) {
        UpdateMovieRequest request = new UpdateMovieRequest();
        // Toggle will be handled by getting current state and inverting
        MovieResponse current = movieService.getMovieById(movieId);
        request.setIsActive(!current.getIsActive());
        return ResponseEntity.ok(adminMovieService.updateMovie(movieId, request));
    }
}
