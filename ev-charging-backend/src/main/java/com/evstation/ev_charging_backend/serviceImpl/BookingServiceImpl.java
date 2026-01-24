package com.evstation.ev_charging_backend.serviceImpl;

import com.evstation.ev_charging_backend.dto.BookingRequestDto;
import com.evstation.ev_charging_backend.dto.BookingResponseDto;
import com.evstation.ev_charging_backend.entity.Booking;
import com.evstation.ev_charging_backend.entity.Charger;
import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.enums.BookingStatus;
import com.evstation.ev_charging_backend.exception.BookingConflictException;
import com.evstation.ev_charging_backend.exception.BookingNotFoundException;
import com.evstation.ev_charging_backend.exception.ResourceNotFoundException;
import com.evstation.ev_charging_backend.repository.BookingRepository;
import com.evstation.ev_charging_backend.repository.ChargerRepository;
import com.evstation.ev_charging_backend.repository.UserRepository;
import com.evstation.ev_charging_backend.service.BookingService;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class BookingServiceImpl implements BookingService {

    private static final long MIN_DURATION_MINUTES = 30;
    private static final long MAX_DURATION_MINUTES = 8 * 60;
    private static final long MIN_ADVANCE_MINUTES = 15;
    private static final long CANCELLATION_DEADLINE_HOURS = 1;

    private final BookingRepository bookingRepository;
    private final ChargerRepository chargerRepository;
    private final UserRepository userRepository;

    public BookingServiceImpl(
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
    public BookingResponseDto createBooking(BookingRequestDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Charger charger = chargerRepository.findByIdForUpdate(dto.getChargerId());
        if (charger == null) {
            throw new ResourceNotFoundException("Charger not found");
        }

        validateTime(dto.getStartTime(), dto.getEndTime());

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                charger.getId(),
                dto.getStartTime(),
                dto.getEndTime(),
                List.of(BookingStatus.CONFIRMED, BookingStatus.ACTIVE)
        );

        if (!conflicts.isEmpty()) {
            throw new BookingConflictException("Charger already booked for selected time");
        }

        Booking booking = Booking.builder()
                .user(user)
                .charger(charger)
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .status(BookingStatus.CONFIRMED)
                .pricePerKwh(charger.getPricePerKwh())
                .build();

        bookingRepository.save(booking);

        return mapToResponse(booking);
    }

    @Override
    public List<BookingResponseDto> getMyBookings(Long userId) {
        return bookingRepository.findByUserUserIdOrderByStartTimeDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<BookingResponseDto> getBookingsByCharger(Long chargerId) {
        return bookingRepository.findByChargerIdOrderByStartTimeAsc(chargerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        if (!booking.getUser().getUserId().equals(userId)) {
            throw new SecurityException("You are not allowed to cancel this booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed booking");
        }

        LocalDateTime cancellationDeadline = booking.getStartTime()
                .minusHours(CANCELLATION_DEADLINE_HOURS);

        if (LocalDateTime.now().isAfter(cancellationDeadline)) {
            throw new IllegalStateException(
                    "Cannot cancel within " + CANCELLATION_DEADLINE_HOURS + " hour(s) of start time"
            );
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    // ✅ FIXED: Host can see all bookings for their chargers
    @Override
    public List<BookingResponseDto> getBookingsByHost(Long hostId) {
        // Use the optimized repository query instead of loading all bookings
        return bookingRepository.findByChargerHostUserIdOrderByStartTimeDesc(hostId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ================= PRIVATE METHODS =================

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

    /**
     * Helper method to map booking status in real-time
     */
    private BookingStatus mapStatusRealTime(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return BookingStatus.CANCELLED;
        }

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(booking.getStartTime())) {
            return BookingStatus.CONFIRMED;
        } else if (now.isBefore(booking.getEndTime())) {
            return BookingStatus.ACTIVE;
        } else {
            return BookingStatus.COMPLETED;
        }
    }

    private BookingResponseDto mapToResponse(Booking booking) {
        BigDecimal totalPrice = null;
        if (booking.getTotalEnergyKwh() != null) {
            totalPrice = booking.getPricePerKwh()
                    .multiply(BigDecimal.valueOf(booking.getTotalEnergyKwh()));
        }

        return BookingResponseDto.builder()
                .bookingId(booking.getId())
                .userId(booking.getUser().getUserId())
                .chargerId(booking.getCharger().getId())
                .chargerName(booking.getCharger().getName())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .status(mapStatusRealTime(booking)) // ✅ Real-time status
                .pricePerKwh(booking.getPricePerKwh())
                .totalPrice(totalPrice)
                .build();
    }
}