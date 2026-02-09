package com.bookmyshow.movie.service;

import com.bookmyshow.movie.dto.MovieResponse;
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
     */
    public Page<MovieResponse> getMovies(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("releaseDate").descending());
        return movieRepository.findByIsActiveTrue(pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get a movie by ID.
     */
    @Cacheable(value = "movies", key = "#movieId")
    public MovieResponse getMovieById(UUID movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", "id", movieId));
        return mapToResponse(movie);
    }

    /**
     * Get movies showing in a specific city.
     */
    public Page<MovieResponse> getMoviesByCity(UUID cityId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return movieRepository.findMoviesByCity(cityId, LocalDate.now(), pageable)
                .map(this::mapToResponse);
    }

    /**
     * Search movies by title.
     */
    public Page<MovieResponse> searchMovies(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return movieRepository.searchByTitle(query, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get featured/trending movies.
     */
    @Cacheable(value = "featured-movies")
    public List<MovieResponse> getFeaturedMovies() {
        return movieRepository.findFeaturedMovies(LocalDate.now(), PageRequest.of(0, 10))
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
