package com.bookmyshow.movie.service;

import com.bookmyshow.movie.config.CacheEventLogger;
import com.bookmyshow.movie.dto.MovieResponse;
import com.bookmyshow.movie.dto.PagedResponse;
import com.bookmyshow.movie.model.Movie;
import com.bookmyshow.movie.repository.MovieRepository;
import com.bookmyshow.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for movie catalog operations.
 */
@Service
@RequiredArgsConstructor
public class MovieService {

    private static final Logger log = LoggerFactory.getLogger(MovieService.class);

    private final MovieRepository movieRepository;

    /**
     * Get all active movies with pagination.
     * Cached for 10 minutes to reduce DB load on homepage.
     */
    @Cacheable(value = "movies-list", key = "'page:' + #page + ':size:' + #size")
    public PagedResponse<MovieResponse> getMovies(int page, int size) {
        CacheEventLogger.logCacheMiss("movies-list", "page", page, "size", size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("releaseDate").descending());
        Page<MovieResponse> pageResult = movieRepository.findByIsActiveTrue(pageable)
                .map(this::mapToResponse);
        return toPagedResponse(pageResult);
    }

    /**
     * Get a movie by ID.
     */
    @Cacheable(value = "movies", key = "#movieId")
    public MovieResponse getMovieById(UUID movieId) {
        CacheEventLogger.logCacheMiss("movies", movieId);
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", "id", movieId));
        return mapToResponse(movie);
    }

    /**
     * Get movies showing in a specific city.
     * Cached for 10 minutes per city.
     */
    @Cacheable(value = "movies-by-city", key = "#cityId + ':page:' + #page + ':size:' + #size")
    public PagedResponse<MovieResponse> getMoviesByCity(UUID cityId, int page, int size) {
        CacheEventLogger.logCacheMiss("movies-by-city", cityId, "page", page, "size", size);
        Pageable pageable = PageRequest.of(page, size);
        Page<MovieResponse> pageResult = movieRepository.findMoviesByCity(cityId, LocalDate.now(), pageable)
                .map(this::mapToResponse);
        return toPagedResponse(pageResult);
    }

    /**
     * Search movies by title.
     * Not cached since search queries are highly variable.
     */
    public PagedResponse<MovieResponse> searchMovies(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MovieResponse> pageResult = movieRepository.searchByTitle(query, pageable)
                .map(this::mapToResponse);
        return toPagedResponse(pageResult);
    }

    /**
     * Get featured/trending movies.
     */
    @Cacheable(value = "featured-movies")
    public List<MovieResponse> getFeaturedMovies() {
        CacheEventLogger.logCacheMiss("featured-movies", "all");
        return movieRepository.findFeaturedMovies(LocalDate.now(), PageRequest.of(0, 10))
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert Spring Data Page to cache-friendly PagedResponse.
     */
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
                .build();
    }
}
