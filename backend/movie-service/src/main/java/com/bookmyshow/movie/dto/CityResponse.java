package com.bookmyshow.movie.dto;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityResponse {
    private UUID id;
    private String name;
    private String state;
    private String country;
}
