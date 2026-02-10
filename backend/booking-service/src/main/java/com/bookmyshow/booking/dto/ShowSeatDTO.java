package com.bookmyshow.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for show seats that includes seat layout info (row, number, type)
 * from the seats table joined with show_seats.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowSeatDTO {
    private UUID id;          // show_seat id
    private UUID showId;
    private UUID seatId;
    private String status;
    private BigDecimal price;
    private String seatRow;
    private String seatNumber;
    private String seatType;
    private int columnNumber;
}
