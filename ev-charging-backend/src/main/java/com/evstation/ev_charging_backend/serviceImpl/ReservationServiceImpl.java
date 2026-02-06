package com.evstation.ev_charging_backend.serviceImpl;

import com.evstation.ev_charging_backend.dto.ReservationRequestDto;
import com.evstation.ev_charging_backend.dto.ReservationResponseDto;
import com.evstation.ev_charging_backend.entity.Booking;
import com.evstation.ev_charging_backend.entity.Charger;
import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.enums.BookingStatus;
import com.evstation.ev_charging_backend.exception.BookingConflictException;
import com.evstation.ev_charging_backend.exception.BookingNotFoundException;
import com.evstation.ev_charging_backend.exception.ReservationExpiredException;
import com.evstation.ev_charging_backend.exception.ResourceNotFoundException;
import com.evstation.ev_charging_backend.repository.BookingRepository;
import com.evstation.ev_charging_backend.repository.ChargerRepository;
import com.evstation.ev_charging_backend.repository.UserRepository;
import com.evstation.ev_charging_backend.service.ReservationService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationServiceImpl implements ReservationService {

    private static final long MIN_DURATION_MINUTES = 30;
    private static final long MAX_DURATION_MINUTES = 8 * 60;
    private static final long MIN_ADVANCE_MINUTES = 15;
    private static final long RESERVATION_TIMEOUT_MINUTES = 3;

    private final BookingRepository bookingRepository;
    private final ChargerRepository chargerRepository;
    private final UserRepository userRepository;

    public ReservationServiceImpl(
            BookingRepository bookingRepository,
            ChargerRepository chargerRepository,
            UserRepository userRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.chargerRepository = chargerRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ReservationResponseDto createReservation(ReservationRequestDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Charger charger = chargerRepository.findByIdForUpdate(dto.getChargerId());
        if (charger == null) {
            throw new ResourceNotFoundException("Charger not found");
        }

        validateTime(dto.getStartTime(), dto.getEndTime());

        // Check for conflicts including RESERVED, PAYMENT_PENDING, CONFIRMED, and ACTIVE bookings
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                charger.getId(),
                dto.getStartTime(),
                dto.getEndTime(),
                List.of(BookingStatus.RESERVED, BookingStatus.PAYMENT_PENDING, 
                       BookingStatus.CONFIRMED, BookingStatus.ACTIVE)
        );

        if (!conflicts.isEmpty()) {
            throw new BookingConflictException("Charger already booked for selected time");
        }

        // Create reservation with 10-minute expiry
        LocalDateTime reservedUntil = LocalDateTime.now().plusMinutes(RESERVATION_TIMEOUT_MINUTES);

        Booking reservation = Booking.builder()
                .user(user)
                .charger(charger)
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .status(BookingStatus.RESERVED)
                .reservedUntil(reservedUntil)
                .pricePerKwh(charger.getPricePerKwh())
                .build();

        bookingRepository.save(reservation);

        return mapToReservationResponse(reservation);
    }

    @Override
    public ReservationResponseDto getReservationById(Long reservationId, Long userId) {
        Booking reservation = bookingRepository.findById(reservationId)
                .orElseThrow(() -> new BookingNotFoundException("Reservation not found"));

        if (!reservation.getUser().getUserId().equals(userId)) {
            throw new SecurityException("You are not allowed to view this reservation");
        }

        checkReservationExpiry(reservationId);

        return mapToReservationResponse(reservation);
    }

    @Override
    public List<ReservationResponseDto> getMyReservations(Long userId) {
        return bookingRepository.findByUserUserIdOrderByStartTimeDesc(userId)
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.RESERVED || 
                            b.getStatus() == BookingStatus.PAYMENT_PENDING)
                .map(this::mapToReservationResponse)
                .toList();
    }

    @Override
    public void validateReservation(Long reservationId, Long userId) {
        Booking reservation = bookingRepository.findById(reservationId)
                .orElseThrow(() -> new BookingNotFoundException("Reservation not found"));

        if (!reservation.getUser().getUserId().equals(userId)) {
            throw new SecurityException("You are not allowed to access this reservation");
        }

        if (reservation.getStatus() == BookingStatus.EXPIRED) {
            throw new ReservationExpiredException("Reservation has expired");
        }

        if (reservation.getStatus() != BookingStatus.RESERVED && 
            reservation.getStatus() != BookingStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("Invalid reservation status");
        }

        if (reservation.getReservedUntil() != null && 
            LocalDateTime.now().isAfter(reservation.getReservedUntil())) {
            reservation.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(reservation);
            throw new ReservationExpiredException("Reservation has expired");
        }
    }

    @Override
    @Transactional
    public void checkReservationExpiry(Long reservationId) {
        Booking reservation = bookingRepository.findById(reservationId)
                .orElseThrow(() -> new BookingNotFoundException("Reservation not found"));

        if (reservation.getStatus() == BookingStatus.RESERVED && 
            reservation.getReservedUntil() != null &&
            LocalDateTime.now().isAfter(reservation.getReservedUntil())) {
            reservation.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(reservation);
        }
    }

    private void validateTime(LocalDateTime start, LocalDateTime end) {
        LocalDateTime now = LocalDateTime.now();

        if (start.isBefore(now)) {
            throw new IllegalArgumentException("Start time must be in the future");
        }

        if (start.isBefore(now.plusMinutes(MIN_ADVANCE_MINUTES))) {
            throw new IllegalArgumentException(
                    "Booking must be at least " + MIN_ADVANCE_MINUTES + " minutes in advance"
            );
        }

        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        long duration = Duration.between(start, end).toMinutes();

        if (duration < MIN_DURATION_MINUTES) {
            throw new IllegalArgumentException(
                    "Minimum booking duration is " + MIN_DURATION_MINUTES + " minutes"
            );
        }

        if (duration > MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException(
                    "Maximum booking duration is " + (MAX_DURATION_MINUTES / 60) + " hours"
            );
        }
    }

    private ReservationResponseDto mapToReservationResponse(Booking booking) {
        long durationMinutes = Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();
        
        // Estimate price based on average consumption (assuming ~7 kWh per hour for EVs)
        double estimatedKwh = (durationMinutes / 60.0) * 7.0;
        BigDecimal estimatedPrice = booking.getPricePerKwh()
                .multiply(BigDecimal.valueOf(estimatedKwh))
                .setScale(2, BigDecimal.ROUND_HALF_UP);

        return ReservationResponseDto.builder()
                .reservationId(booking.getId())
                .userId(booking.getUser().getUserId())
                .chargerId(booking.getCharger().getId())
                .chargerName(booking.getCharger().getName())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .status(booking.getStatus())
                .reservedUntil(booking.getReservedUntil())
                .estimatedPrice(estimatedPrice)
                .pricePerKwh(booking.getPricePerKwh())
                .durationMinutes(durationMinutes)
                .build();
    }
}