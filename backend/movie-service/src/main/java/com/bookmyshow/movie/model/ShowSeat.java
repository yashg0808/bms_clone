package com.bookmyshow.movie.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ShowSeat entity - represents a single seat's availability for a specific show.
 * This is a per-show instance created from the Seat template.
 */
@Entity
@Table(name = "show_seats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ShowSeat {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "show_id", nullable = false)
    private UUID showId;

    @Column(name = "seat_id", nullable = false)
    private UUID seatId;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "AVAILABLE";

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "locked_by")
    private UUID lockedBy;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
