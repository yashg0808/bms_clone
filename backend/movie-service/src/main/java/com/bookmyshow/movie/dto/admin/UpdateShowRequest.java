package com.bookmyshow.movie.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Request DTO for updating an existing show.
 * All fields are optional - only provided fields will be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShowRequest {

    private UUID movieId;

    private UUID screenId;

    private LocalDate showDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private BigDecimal basePrice;

    private BigDecimal premiumPrice;

    private BigDecimal reclinerPrice;

    private Boolean isActive;
}
