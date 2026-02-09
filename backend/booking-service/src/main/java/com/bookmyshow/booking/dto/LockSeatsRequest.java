package com.bookmyshow.booking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockSeatsRequest {

    @NotNull(message = "Show ID is required")
    private UUID showId;

    @NotEmpty(message = "At least one seat must be selected")
    private List<UUID> seatIds;
}
