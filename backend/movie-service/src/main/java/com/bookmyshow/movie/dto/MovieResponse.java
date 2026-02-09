package com.bookmyshow.movie.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse {
    private UUID id;
    private String title;
    private String description;
    private Integer durationMinutes;
    private String language;
    private String genre;
    private LocalDate releaseDate;
    private String rating;
    private BigDecimal imdbRating;
    private String posterUrl;
    private String bannerUrl;
    private String trailerUrl;
    private String castInfo;
    private String crewInfo;
}
