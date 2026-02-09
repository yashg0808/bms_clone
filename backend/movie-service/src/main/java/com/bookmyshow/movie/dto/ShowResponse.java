package com.bookmyshow.movie.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowResponse {
    private UUID id;
    private UUID movieId;
    private String movieTitle;
    private UUID screenId;
    private String screenName;
    private String screenType;
    private String theaterName;
    private UUID theaterId;
    private LocalDate showDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal basePrice;
    private BigDecimal premiumPrice;
    private BigDecimal reclinerPrice;
    private Integer availableSeats;
}
