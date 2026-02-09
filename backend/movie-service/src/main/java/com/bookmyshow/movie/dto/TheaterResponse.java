package com.bookmyshow.movie.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TheaterResponse {
    private UUID id;
    private String name;
    private String address;
    private String cityName;
    private Integer totalScreens;
    private List<ScreenResponse> screens;
}
