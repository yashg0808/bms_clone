package com.bookmyshow.movie.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating a new screen.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateScreenRequest {

    @NotNull(message = "Theater ID is required")
    private UUID theaterId;

    @NotBlank(message = "Screen name is required")
    private String name;

    @NotNull(message = "Total seats is required")
    @Positive(message = "Total seats must be positive")
    private Integer totalSeats;

    @Builder.Default
    private String screenType = "STANDARD";

    @Builder.Default
    private Boolean isActive = true;
}
