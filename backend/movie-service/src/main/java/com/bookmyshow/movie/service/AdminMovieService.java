package com.bookmyshow.movie.service;

import com.bookmyshow.movie.dto.MovieResponse;
import com.bookmyshow.movie.dto.PagedResponse;
import com.bookmyshow.movie.dto.admin.CreateMovieRequest;
import com.bookmyshow.movie.dto.admin.UpdateMovieRequest;
import com.bookmyshow.movie.model.Movie;
import com.bookmyshow.movie.repository.MovieRepository;
import com.bookmyshow.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Admin service for movie management operations.
 */
@Service
@RequiredArgsConstructor
public class AdminMovieService {

    private static final Logger log = LoggerFactory.getLogger(AdminMovieService.class);

    private final MovieRepository movieRepository;

    /**
     * Get all movies (including inactive) for admin panel.
     */
    public PagedResponse<MovieResponse> getAllMovies(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<MovieResponse> pageResult = movieRepository.findAll(pageable)
                .map(this::mapToResponse);
        
        return toPagedResponse(pageResult);
    }

    /**
     * Create a new movie.
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "movies-list", allEntries = true),
        @CacheEvict(value = "movies-by-city", allEntries = true),
        @CacheEvict(value = "featured-movies", allEntries = true)
    })
    public MovieResponse createMovie(CreateMovieRequest request) {
        log.info("Creating new movie: {}", request.getTitle());
        
        Movie movie = Movie.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .durationMinutes(request.getDurationMinutes())
                .language(request.getLanguage())
                .genre(request.getGenre())
                .releaseDate(request.getReleaseDate())
                .endDate(request.getEndDate())
                .rating(request.getRating())
                .imdbRating(request.getImdbRating())
                .posterUrl(request.getPosterUrl())
                .bannerUrl(request.getBannerUrl())
                .trailerUrl(request.getTrailerUrl())
                .castInfo(request.getCastInfo())
                .crewInfo(request.getCrewInfo())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        
        Movie saved = movieRepository.save(movie);
        log.info("Created movie with ID: {}", saved.getId());
        
        return mapToResponse(saved);
    }

    /**
     * Update an existing movie.
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "movies", key = "#movieId"),
        @CacheEvict(value = "movies-list", allEntries = true),
        @CacheEvict(value = "movies-by-city", allEntries = true),
        @CacheEvict(value = "featured-movies", allEntries = true)
    })
    public MovieResponse updateMovie(UUID movieId, UpdateMovieRequest request) {
        log.info("Updating movie: {}", movieId);
        
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", "id", movieId));
        
        // Update only non-null fields
        if (request.getTitle() != null) movie.setTitle(request.getTitle());
        if (request.getDescription() != null) movie.setDescription(request.getDescription());
        if (request.getDurationMinutes() != null) movie.setDurationMinutes(request.getDurationMinutes());
        if (request.getLanguage() != null) movie.setLanguage(request.getLanguage());
        if (request.getGenre() != null) movie.setGenre(request.getGenre());
        if (request.getReleaseDate() != null) movie.setReleaseDate(request.getReleaseDate());
        if (request.getEndDate() != null) movie.setEndDate(request.getEndDate());
        if (request.getRating() != null) movie.setRating(request.getRating());
        if (request.getImdbRating() != null) movie.setImdbRating(request.getImdbRating());
        if (request.getPosterUrl() != null) movie.setPosterUrl(request.getPosterUrl());
        if (request.getBannerUrl() != null) movie.setBannerUrl(request.getBannerUrl());
        if (request.getTrailerUrl() != null) movie.setTrailerUrl(request.getTrailerUrl());
        if (request.getCastInfo() != null) movie.setCastInfo(request.getCastInfo());
        if (request.getCrewInfo() != null) movie.setCrewInfo(request.getCrewInfo());
        if (request.getIsActive() != null) movie.setIsActive(request.getIsActive());
        
        Movie saved = movieRepository.save(movie);
        log.info("Updated movie: {}", saved.getId());
        
        return mapToResponse(saved);
    }

    /**
     * Delete a movie (soft delete by setting isActive = false).
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "movies", key = "#movieId"),
        @CacheEvict(value = "movies-list", allEntries = true),
        @CacheEvict(value = "movies-by-city", allEntries = true),
        @CacheEvict(value = "featured-movies", allEntries = true)
    })
    public void deleteMovie(UUID movieId) {
        log.info("Deleting (deactivating) movie: {}", movieId);
        
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", "id", movieId));
        
        movie.setIsActive(false);
        movieRepository.save(movie);
        
        log.info("Deactivated movie: {}", movieId);
    }

    /**
     * Hard delete a movie (permanent).
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "movies", key = "#movieId"),
        @CacheEvict(value = "movies-list", allEntries = true),
        @CacheEvict(value = "movies-by-city", allEntries = true),
        @CacheEvict(value = "featured-movies", allEntries = true)
    })
    public void hardDeleteMovie(UUID movieId) {
        log.warn("HARD DELETING movie: {} - This action is irreversible!", movieId);
        
        if (!movieRepository.existsById(movieId)) {
            throw new ResourceNotFoundException("Movie", "id", movieId);
        }
        
        movieRepository.deleteById(movieId);
        log.info("Permanently deleted movie: {}", movieId);
    }

    private <T> PagedResponse<T> toPagedResponse(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private MovieResponse mapToResponse(Movie movie) {
        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .durationMinutes(movie.getDurationMinutes())
                .language(movie.getLanguage())
                .genre(movie.getGenre())
                .releaseDate(movie.getReleaseDate())
                .rating(movie.getRating())
                .imdbRating(movie.getImdbRating())
                .posterUrl(movie.getPosterUrl())
                .bannerUrl(movie.getBannerUrl())
                .trailerUrl(movie.getTrailerUrl())
                .castInfo(movie.getCastInfo())
                .crewInfo(movie.getCrewInfo())
                .isActive(movie.getIsActive())
                .build();
    }
}
