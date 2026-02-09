package com.bookmyshow.movie.dto;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenResponse {
    private UUID id;
    private String name;
    private Integer totalSeats;
    private String screenType;
}
