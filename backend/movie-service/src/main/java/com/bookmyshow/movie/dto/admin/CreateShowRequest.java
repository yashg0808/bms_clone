package com.bookmyshow.movie.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Request DTO for creating a new show.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShowRequest {

    @NotNull(message = "Movie ID is required")
    private UUID movieId;

    @NotNull(message = "Screen ID is required")
    private UUID screenId;

    @NotNull(message = "Show date is required")
    private LocalDate showDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Base price is required")
    @Positive(message = "Base price must be positive")
    private BigDecimal basePrice;

    private BigDecimal premiumPrice;

    private BigDecimal reclinerPrice;

    @Builder.Default
    private Boolean isActive = true;
}
