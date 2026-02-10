package com.bookmyshow.booking.service;

import com.bookmyshow.booking.dto.BookingResponse;
import com.bookmyshow.booking.dto.LockSeatsRequest;
import com.bookmyshow.booking.dto.LockSeatsResponse;
import com.bookmyshow.booking.dto.SeatStatusResponse;
import com.bookmyshow.booking.dto.ShowSeatDTO;
import com.bookmyshow.booking.exception.BookingExpiredException;
import com.bookmyshow.booking.exception.InvalidLockTokenException;
import com.bookmyshow.booking.exception.SeatUnavailableException;
import com.bookmyshow.booking.model.*;
import com.bookmyshow.booking.repository.BookingRepository;
import com.bookmyshow.booking.repository.BookingSeatRepository;
import com.bookmyshow.booking.repository.ShowSeatRepository;
import com.bookmyshow.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * BookingService - Orchestrates the complete booking flow (guest model, no authentication):
 *
 * 1. Lock seats → Creates a pending booking
 * 2. Confirm booking → Validates lock, saves guest details, marks seats as BOOKED, publishes Kafka event
 * 3. Cancel booking → Releases seats, updates status
 *
 * Uses SeatLockService for distributed locking and KafkaTemplate for event-driven notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final SeatLockService seatLockService;
    private final SeatCacheService seatCacheService;
    private final ShowSeatRepository showSeatRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${booking.seat-lock.timeout-minutes:8}")
    private int seatLockTimeoutMinutes;

    @Value("${booking.convenience-fee-percent:4.5}")
    private double convenienceFeePercent;

    private static final String KAFKA_TOPIC_BOOKING_CONFIRMED = "booking.confirmed";
    private static final String KAFKA_TOPIC_BOOKING_CANCELLED = "booking.cancelled";

    /**
     * Step 1: Lock seats and create a pending booking.
     * No user authentication required - anyone can lock available seats.
     */
    @Transactional
    public LockSeatsResponse lockSeatsAndCreateBooking(LockSeatsRequest request) {
        // Validate seat limit
        if (request.getSeatIds().size() > 10) {
            throw new IllegalArgumentException("Maximum 10 seats can be booked at once");
        }

        // Lock seats via distributed lock service (no userId needed)
        String lockToken = seatLockService.lockSeats(request.getShowId(), request.getSeatIds());

        // Fetch locked seats for pricing
        List<ShowSeat> lockedSeats = showSeatRepository.findByShowIdAndIdIn(request.getShowId(), request.getSeatIds());

        // Fetch seat layout info (row, number, type) from the seats table
        Map<UUID, ShowSeatDTO> seatInfoMap = mapSeatInfoByShowSeatId(
                showSeatRepository.findSeatInfoByShowSeatIds(request.getSeatIds()));

        // Calculate total
        BigDecimal totalAmount = lockedSeats.stream()
                .map(ShowSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal convenienceFee = totalAmount
                .multiply(BigDecimal.valueOf(convenienceFeePercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal finalAmount = totalAmount.add(convenienceFee);

        // Generate booking number
        String bookingNumber = generateBookingNumber();

        // Create booking entity (guest details will be filled on confirm)
        Booking booking = Booking.builder()
                .bookingNumber(bookingNumber)
                .showId(request.getShowId())
                .status(BookingStatus.PENDING)
                .totalAmount(totalAmount)
                .convenienceFee(convenienceFee)
                .finalAmount(finalAmount)
                .lockToken(lockToken)
                .expiresAt(LocalDateTime.now().plusMinutes(seatLockTimeoutMinutes))
                .build();

        booking = bookingRepository.save(booking);

        // Create booking seat records with real seat info
        for (ShowSeat showSeat : lockedSeats) {
            ShowSeatDTO info = seatInfoMap.get(showSeat.getId());
            BookingSeat bookingSeat = BookingSeat.builder()
                    .showSeatId(showSeat.getId())
                    .seatNumber(info != null ? info.getSeatNumber() : "?")
                    .seatRow(info != null ? info.getSeatRow() : "?")
                    .seatType(info != null ? info.getSeatType() : "REGULAR")
                    .price(showSeat.getPrice())
                    .build();
            booking.addBookingSeat(bookingSeat);
        }

        bookingRepository.save(booking);

        // Build response
        List<LockSeatsResponse.LockedSeatInfo> seatInfos = lockedSeats.stream()
                .map(ss -> {
                    ShowSeatDTO info = seatInfoMap.get(ss.getId());
                    return LockSeatsResponse.LockedSeatInfo.builder()
                        .seatId(ss.getId())
                        .seatRow(info != null ? info.getSeatRow() : "?")
                        .seatNumber(info != null ? info.getSeatNumber() : "?")
                        .seatType(info != null ? info.getSeatType() : "REGULAR")
                        .price(ss.getPrice())
                        .build();
                })
                .collect(Collectors.toList());

        log.info("Booking created - bookingNumber: {}, showId: {}, seats: {}, total: {}",
                bookingNumber, request.getShowId(), request.getSeatIds().size(), finalAmount);

        return LockSeatsResponse.builder()
                .lockToken(lockToken)
                .bookingId(booking.getId())
                .showId(request.getShowId())
                .lockedSeats(seatInfos)
                .totalAmount(finalAmount)
                .expiresAt(booking.getExpiresAt())
                .build();
    }

    /**
     * Step 2: Confirm booking with guest details.
     * Validates lock token, saves guest info, marks seats as BOOKED, publishes Kafka event.
     */
    @Transactional
    public BookingResponse confirmBooking(UUID bookingId, String lockToken,
                                          String guestName, String guestEmail, String guestPhone) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        // Validate booking status
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking is not in PENDING state. Current: " + booking.getStatus());
        }

        // Validate lock token
        if (!lockToken.equals(booking.getLockToken())) {
            throw new InvalidLockTokenException("Invalid lock token for this booking");
        }

        // Check if booking has expired
        if (booking.getExpiresAt() != null && LocalDateTime.now().isAfter(booking.getExpiresAt())) {
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            seatLockService.releaseSeats(lockToken);
            throw new BookingExpiredException("Booking has expired. Seats have been released.");
        }

        // Validate lock is still valid in Redis
        List<UUID> lockedSeatIds = seatLockService.validateLockToken(lockToken);
        if (lockedSeatIds == null || lockedSeatIds.isEmpty()) {
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            throw new BookingExpiredException("Seat lock has expired. Please try booking again.");
        }

        // Mark seats as BOOKED
        List<ShowSeat> seats = showSeatRepository.findByShowIdAndIdIn(booking.getShowId(), lockedSeatIds);
        Map<UUID, String> statusUpdates = new HashMap<>();
        Map<UUID, BigDecimal> priceMap = new HashMap<>();
        for (ShowSeat seat : seats) {
            if (seat.getStatus() != SeatStatus.LOCKED) {
                throw new SeatUnavailableException("Seat status changed unexpectedly. Please try again.");
            }
            seat.setStatus(SeatStatus.BOOKED);
            seat.setLockedBy(null);
            seat.setLockedAt(null);
            statusUpdates.put(seat.getId(), SeatStatus.BOOKED.name());
            priceMap.put(seat.getId(), seat.getPrice());
        }
        showSeatRepository.saveAll(seats);

        // Write-through: update cache to reflect BOOKED status
        seatCacheService.updateSeatStatuses(booking.getShowId(), statusUpdates, priceMap);

        // Save guest details and update booking status
        booking.setGuestName(guestName);
        booking.setGuestEmail(guestEmail);
        booking.setGuestPhone(guestPhone);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setLockToken(null);
        booking = bookingRepository.save(booking);

        // Delete lock token from Redis
        seatLockService.deleteLockToken(lockToken);

        // Publish booking confirmed event to Kafka
        publishBookingEvent(KAFKA_TOPIC_BOOKING_CONFIRMED, booking);

        log.info("Booking confirmed - bookingNumber: {}, guestEmail: {}", booking.getBookingNumber(), guestEmail);

        return mapToBookingResponse(booking);
    }

    /**
     * Cancel a booking and release seats.
     */
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }

        // If pending, release locks
        if (booking.getStatus() == BookingStatus.PENDING && booking.getLockToken() != null) {
            seatLockService.releaseSeats(booking.getLockToken());
        }

        // If confirmed, mark seats available again
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            List<BookingSeat> bookingSeats = booking.getBookingSeats();
            List<UUID> seatIds = bookingSeats.stream()
                    .map(BookingSeat::getShowSeatId)
                    .collect(Collectors.toList());

            List<ShowSeat> showSeats = showSeatRepository.findByShowIdAndIdIn(booking.getShowId(), seatIds);
            Map<UUID, String> statusUpdates = new HashMap<>();
            Map<UUID, BigDecimal> priceMap = new HashMap<>();
            for (ShowSeat seat : showSeats) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setLockedBy(null);
                seat.setLockedAt(null);
                statusUpdates.put(seat.getId(), SeatStatus.AVAILABLE.name());
                priceMap.put(seat.getId(), seat.getPrice());
            }
            showSeatRepository.saveAll(showSeats);

            // Write-through: update cache to reflect AVAILABLE status
            seatCacheService.updateSeatStatuses(booking.getShowId(), statusUpdates, priceMap);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setLockToken(null);
        booking = bookingRepository.save(booking);

        // Publish cancellation event
        publishBookingEvent(KAFKA_TOPIC_BOOKING_CANCELLED, booking);

        log.info("Booking cancelled - bookingNumber: {}", booking.getBookingNumber());

        return mapToBookingResponse(booking);
    }

    /**
     * Get booking by ID.
     */
    @Transactional(readOnly = true)
    public BookingResponse getBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        return mapToBookingResponse(booking);
    }

    /**
     * Get booking by booking number.
     */
    @Transactional(readOnly = true)
    public BookingResponse getBookingByNumber(String bookingNumber) {
        Booking booking = bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "bookingNumber", bookingNumber));

        return mapToBookingResponse(booking);
    }

    /**
     * Get seat availability for a show with full layout info.
     * Uses read-through cache: Redis Hash → on MISS → Postgres → populate cache.
     */
    @Transactional(readOnly = true)
    public List<ShowSeatDTO> getShowSeats(UUID showId) {
        // 1. Always fetch layout + status from DB (layout data is not in cache)
        List<Object[]> rows = showSeatRepository.findShowSeatsWithSeatInfo(showId);
        List<ShowSeatDTO> dbSeats = rows.stream().map(this::mapRowToShowSeatDTO).collect(Collectors.toList());

        if (dbSeats.isEmpty()) {
            return dbSeats;
        }

        // 2. Try to overlay status from Redis cache (much faster for repeat reads)
        Map<String, String> cached = seatCacheService.getShowSeatStatuses(showId);

        if (!cached.isEmpty()) {
            // Cache HIT — overlay the cached status onto the DB layout
            for (ShowSeatDTO seat : dbSeats) {
                String cachedValue = cached.get(seat.getId().toString());
                if (cachedValue != null) {
                    String[] parts = cachedValue.split(":", 2);
                    seat.setStatus(parts[0]);
                    // price from cache for consistency
                    if (parts.length > 1) {
                        try {
                            seat.setPrice(new java.math.BigDecimal(parts[1]));
                        } catch (NumberFormatException ignored) { }
                    }
                }
            }
        } else {
            // Cache MISS — populate cache from the DB result set
            seatCacheService.populateCache(showId, dbSeats);
        }

        return dbSeats;
    }

    /**
     * Get available seat count for a show.
     */
    @Transactional(readOnly = true)
    public long getAvailableSeatCount(UUID showId) {
        return showSeatRepository.countByShowIdAndStatus(showId, SeatStatus.AVAILABLE);
    }

    /**
     * Get lightweight seat statuses for a show (no layout data).
     * For CDN-decoupled flow: frontend fetches layout from static JSON, status from this method.
     * Uses the same read-through cache as getShowSeats.
     */
    @Transactional(readOnly = true)
    public SeatStatusResponse getShowSeatStatuses(UUID showId) {
        // Try Redis cache first
        Map<String, String> cached = seatCacheService.getShowSeatStatuses(showId);

        if (!cached.isEmpty()) {
            // Cache HIT — build response from cache
            List<SeatStatusResponse.SeatStatus> statuses = new ArrayList<>();
            // We need seatId (template) mapping — cache only has showSeatId. Query lightweight.
            List<Object[]> rows = showSeatRepository.findShowSeatsWithSeatInfo(showId);
            for (Object[] row : rows) {
                UUID showSeatId = (UUID) row[0];
                UUID seatId = (UUID) row[2];
                String cachedValue = cached.get(showSeatId.toString());
                String status;
                BigDecimal price;
                if (cachedValue != null) {
                    String[] parts = cachedValue.split(":", 2);
                    status = parts[0];
                    price = parts.length > 1 ? new BigDecimal(parts[1]) : (BigDecimal) row[4];
                } else {
                    status = (String) row[3];
                    price = (BigDecimal) row[4];
                }
                statuses.add(SeatStatusResponse.SeatStatus.builder()
                        .showSeatId(showSeatId)
                        .seatId(seatId)
                        .status(status)
                        .price(price)
                        .build());
            }
            return SeatStatusResponse.builder().showId(showId).seats(statuses).build();
        }

        // Cache MISS — query DB, populate cache, return
        List<Object[]> rows = showSeatRepository.findShowSeatsWithSeatInfo(showId);
        List<ShowSeatDTO> dtoList = rows.stream().map(this::mapRowToShowSeatDTO).collect(Collectors.toList());
        seatCacheService.populateCache(showId, dtoList);

        List<SeatStatusResponse.SeatStatus> statuses = dtoList.stream()
                .map(dto -> SeatStatusResponse.SeatStatus.builder()
                        .showSeatId(dto.getId())
                        .seatId(dto.getSeatId())
                        .status(dto.getStatus())
                        .price(dto.getPrice())
                        .build())
                .collect(Collectors.toList());

        return SeatStatusResponse.builder().showId(showId).seats(statuses).build();
    }

    // ---- Private helpers ----

    private ShowSeatDTO mapRowToShowSeatDTO(Object[] row) {
        return ShowSeatDTO.builder()
                .id((UUID) row[0])
                .showId((UUID) row[1])
                .seatId((UUID) row[2])
                .status((String) row[3])
                .price((BigDecimal) row[4])
                .seatRow((String) row[5])
                .seatNumber((String) row[6])
                .seatType((String) row[7])
                .columnNumber(((Number) row[8]).intValue())
                .build();
    }

    private Map<UUID, ShowSeatDTO> mapSeatInfoByShowSeatId(List<Object[]> rows) {
        return rows.stream()
                .map(this::mapRowToShowSeatDTO)
                .collect(Collectors.toMap(ShowSeatDTO::getId, Function.identity()));
    }

    private String generateBookingNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "BMS-" + timestamp + "-" + random;
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        List<BookingResponse.BookingSeatInfo> seatInfos = booking.getBookingSeats().stream()
                .map(bs -> BookingResponse.BookingSeatInfo.builder()
                        .seatId(bs.getShowSeatId())
                        .seatRow(bs.getSeatRow())
                        .seatNumber(bs.getSeatNumber())
                        .seatType(bs.getSeatType())
                        .price(bs.getPrice())
                        .build())
                .collect(Collectors.toList());

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .guestName(booking.getGuestName())
                .guestEmail(booking.getGuestEmail())
                .guestPhone(booking.getGuestPhone())
                .showId(booking.getShowId())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .convenienceFee(booking.getConvenienceFee())
                .discount(booking.getDiscount())
                .finalAmount(booking.getFinalAmount())
                .lockToken(booking.getLockToken())
                .expiresAt(booking.getExpiresAt())
                .seats(seatInfos)
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private void publishBookingEvent(String topic, Booking booking) {
        try {
            String eventPayload = String.format(
                    "{\"bookingId\":\"%s\",\"bookingNumber\":\"%s\",\"guestEmail\":\"%s\",\"guestName\":\"%s\",\"showId\":\"%s\",\"status\":\"%s\",\"finalAmount\":%s,\"seatCount\":%d}",
                    booking.getId(), booking.getBookingNumber(),
                    booking.getGuestEmail() != null ? booking.getGuestEmail() : "",
                    booking.getGuestName() != null ? booking.getGuestName() : "",
                    booking.getShowId(), booking.getStatus(), booking.getFinalAmount(),
                    booking.getBookingSeats().size()
            );
            kafkaTemplate.send(topic, booking.getId().toString(), eventPayload);
            log.info("Published event to {}: {}", topic, booking.getBookingNumber());
        } catch (Exception e) {
            log.error("Failed to publish booking event to {}: {}", topic, e.getMessage());
            // Don't fail the booking if Kafka publish fails - use outbox pattern in production
        }
    }
}
