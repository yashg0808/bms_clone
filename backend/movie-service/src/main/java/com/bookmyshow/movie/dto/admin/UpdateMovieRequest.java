package com.bookmyshow.movie.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for updating an existing movie.
 * All fields are optional - only provided fields will be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMovieRequest {

    private String title;

    private String description;

    private Integer durationMinutes;

    private String language;

    private String genre;

    private LocalDate releaseDate;

    private LocalDate endDate;

    private String rating;

    private BigDecimal imdbRating;

    private String posterUrl;

    private String bannerUrl;

    private String trailerUrl;

    private String castInfo;

    private String crewInfo;

    private Boolean isActive;
}
