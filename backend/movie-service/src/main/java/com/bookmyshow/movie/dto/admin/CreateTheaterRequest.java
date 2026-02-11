package com.bookmyshow.movie.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating a new theater.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTheaterRequest {

    @NotBlank(message = "Theater name is required")
    private String name;

    @NotNull(message = "City ID is required")
    private UUID cityId;

    @NotBlank(message = "Address is required")
    private String address;

    private String phone;

    private Integer totalScreens;

    @Builder.Default
    private Boolean isActive = true;
}
