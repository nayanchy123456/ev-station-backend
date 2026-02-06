package com.evstation.ev_charging_backend.serviceImpl;

import com.evstation.ev_charging_backend.dto.BookingRequestDto;
import com.evstation.ev_charging_backend.dto.BookingResponseDto;
import com.evstation.ev_charging_backend.entity.Booking;
import com.evstation.ev_charging_backend.entity.Charger;
import com.evstation.ev_charging_backend.entity.Conversation;
import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.entity.Rating; // ⭐ NEW
import com.evstation.ev_charging_backend.enums.BookingStatus;
import com.evstation.ev_charging_backend.exception.BookingConflictException;
import com.evstation.ev_charging_backend.exception.BookingNotFoundException;
import com.evstation.ev_charging_backend.exception.ResourceNotFoundException;
import com.evstation.ev_charging_backend.repository.BookingRepository;
import com.evstation.ev_charging_backend.repository.ChargerRepository;
import com.evstation.ev_charging_backend.repository.ConversationRepository;
import com.evstation.ev_charging_backend.repository.UserRepository;
import com.evstation.ev_charging_backend.repository.RatingRepository; // ⭐ NEW
import com.evstation.ev_charging_backend.service.BookingService;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional; // ⭐ NEW

import org.springframework.stereotype.Service;

/**
 * ✅ UPDATED BookingServiceImpl with Rating Information
 * 
 * CHANGES:
 * 1. Added RatingRepository dependency
 * 2. Updated mapToResponse() to include rating information
 * 3. Ratings are fetched and included in booking responses
 */
@Service
@Slf4j
public class BookingServiceImpl implements BookingService {

    private static final long MIN_DURATION_MINUTES = 30;
    private static final long MAX_DURATION_MINUTES = 8 * 60;
    private static final long MIN_ADVANCE_MINUTES = 15;
    private static final long CANCELLATION_DEADLINE_HOURS = 1;
    private static final long RESERVATION_TIMEOUT_MINUTES = 3;

    private final BookingRepository bookingRepository;
    private final ChargerRepository chargerRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final RatingRepository ratingRepository; // ⭐ NEW

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            ChargerRepository chargerRepository,
            UserRepository userRepository,
            ConversationRepository conversationRepository,
            RatingRepository ratingRepository // ⭐ NEW
    ) {
        this.bookingRepository = bookingRepository;
        this.chargerRepository = chargerRepository;
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.ratingRepository = ratingRepository; // ⭐ NEW
    }

    /**
     * Create Booking - Follows reservation flow
     * 
     * CHANGES:
     * - Status starts as RESERVED (not CONFIRMED)
     * - Sets reservedUntil timestamp
     * - Logs host ID for debugging
     * - Auto-creates conversation between user and host
     * 
     * @param dto Booking request data
     * @param userId ID of the user creating the booking
     * @return BookingResponseDto
     */
    @Override
    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto dto, Long userId) {
        log.info("📝 Creating booking for user {} at charger {}", userId, dto.getChargerId());
        
        // Load user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Load charger with pessimistic lock to prevent double-booking
        Charger charger = chargerRepository.findByIdForUpdate(dto.getChargerId());
        if (charger == null) {
            throw new ResourceNotFoundException("Charger not found");
        }

        // Get the host of the charger
        User host = charger.getHost();
        if (host == null) {
            log.error("❌ Charger {} has no host assigned", charger.getId());
            throw new ResourceNotFoundException("Charger host not found");
        }

        log.info("🏠 Charger host ID: {}", host.getUserId());

        // Validate booking time
        validateTime(dto.getStartTime(), dto.getEndTime());

        // Check for conflicts
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

        // Create booking with RESERVED status
        LocalDateTime reservedUntil = LocalDateTime.now().plusMinutes(RESERVATION_TIMEOUT_MINUTES);
        
        Booking booking = Booking.builder()
                .user(user)
                .charger(charger)
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .status(BookingStatus.RESERVED)
                .reservedUntil(reservedUntil)
                .pricePerKwh(charger.getPricePerKwh())
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        
        log.info("✅ Booking #{} created with status RESERVED for user {} on charger {} (host: {})", 
                 savedBooking.getId(), userId, charger.getId(), host.getUserId());
        log.info("⏰ Reservation expires at: {}", reservedUntil);

        // Auto-create conversation between user and host
        try {
            createOrGetConversation(userId, host.getUserId());
        } catch (Exception e) {
            log.error("⚠️ Failed to create conversation between user {} and host {}: {}", 
                     userId, host.getUserId(), e.getMessage(), e);
        }

        return mapToResponse(savedBooking);
    }

    @Override
    public List<BookingResponseDto> getMyBookings(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserUserIdOrderByStartTimeDesc(userId);
        log.debug("📊 User {} has {} total bookings", userId, bookings.size());
        
        return bookings.stream()
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
        
        if (booking.getStatus() == BookingStatus.EXPIRED) {
            throw new IllegalStateException("Cannot cancel expired reservation");
        }

        // Allow cancellation of RESERVED and PAYMENT_PENDING without time restrictions
        if (booking.getStatus() == BookingStatus.RESERVED || 
            booking.getStatus() == BookingStatus.PAYMENT_PENDING) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            
            log.info("🚫 User {} cancelled booking #{} (was {})", 
                     userId, bookingId, booking.getStatus());
            return;
        }

        // For CONFIRMED bookings, check the cancellation deadline
        LocalDateTime cancellationDeadline = booking.getStartTime()
                .minusHours(CANCELLATION_DEADLINE_HOURS);

        if (LocalDateTime.now().isAfter(cancellationDeadline)) {
            throw new IllegalStateException(
                    "Cannot cancel within " + CANCELLATION_DEADLINE_HOURS + " hour(s) of start time"
            );
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        
        log.info("🚫 User {} cancelled confirmed booking #{}", userId, bookingId);
    }

    @Override
    public List<BookingResponseDto> getBookingsByHost(Long hostId) {
        List<Booking> bookings = bookingRepository.findByChargerHostUserIdOrderByStartTimeDesc(hostId);
        
        long reservedCount = bookings.stream().filter(b -> b.getStatus() == BookingStatus.RESERVED).count();
        long paymentPendingCount = bookings.stream().filter(b -> b.getStatus() == BookingStatus.PAYMENT_PENDING).count();
        long confirmedCount = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        
        log.info("📊 Host {} bookings: Total={}, RESERVED={}, PAYMENT_PENDING={}, CONFIRMED={}", 
                 hostId, bookings.size(), reservedCount, paymentPendingCount, confirmedCount);
        
        return bookings.stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ================= PRIVATE METHODS =================

    private Conversation createOrGetConversation(Long userId, Long hostId) {
        if (userId.equals(hostId)) {
            log.debug("ℹ️ User {} is booking their own charger, skipping conversation creation", userId);
            return null;
        }

        return conversationRepository.findByUsers(userId, hostId)
            .orElseGet(() -> {
                Long user1Id = Math.min(userId, hostId);
                Long user2Id = Math.max(userId, hostId);
                
                return conversationRepository.findByUsers(user1Id, user2Id)
                    .orElseGet(() -> {
                        Conversation newConversation = Conversation.builder()
                            .user1Id(user1Id)
                            .user2Id(user2Id)
                            .lastMessage(null)
                            .lastMessageTime(null)
                            .unreadCountUser1(0)
                            .unreadCountUser2(0)
                            .build();
                        
                        Conversation saved = conversationRepository.save(newConversation);
                        log.info("✨ Auto-created conversation {} between user {} and host {}", 
                                 saved.getId(), userId, hostId);
                        return saved;
                    });
            });
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

    private BookingStatus mapStatusRealTime(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED ||
            booking.getStatus() == BookingStatus.RESERVED ||
            booking.getStatus() == BookingStatus.PAYMENT_PENDING ||
            booking.getStatus() == BookingStatus.EXPIRED) {
            return booking.getStatus();
        }

        LocalDateTime now = LocalDateTime.now();

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            if (now.isBefore(booking.getStartTime())) {
                return BookingStatus.CONFIRMED;
            } else if (now.isBefore(booking.getEndTime())) {
                return BookingStatus.ACTIVE;
            } else {
                return BookingStatus.COMPLETED;
            }
        }

        return booking.getStatus();
    }

    /**
     * ⭐ UPDATED: Map Booking to Response DTO with Rating Information
     * 
     * NEW: Fetches rating information if exists and includes it in response
     */
    private BookingResponseDto mapToResponse(Booking booking) {
        BigDecimal totalPrice = null;
        if (booking.getTotalEnergyKwh() != null) {
            totalPrice = booking.getPricePerKwh()
                    .multiply(BigDecimal.valueOf(booking.getTotalEnergyKwh()));
        } else if (booking.getTotalPrice() != null) {
            totalPrice = booking.getTotalPrice();
        }

        // ⭐ NEW - Fetch rating information if exists
        Optional<Rating> ratingOpt = ratingRepository.findByBookingId(booking.getId());
        
        BookingResponseDto.BookingResponseDtoBuilder builder = BookingResponseDto.builder()
                .bookingId(booking.getId())
                .userId(booking.getUser().getUserId())
                .chargerId(booking.getCharger().getId())
                .chargerName(booking.getCharger().getName())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .status(mapStatusRealTime(booking))
                .reservedUntil(booking.getReservedUntil())
                .pricePerKwh(booking.getPricePerKwh())
                .totalPrice(totalPrice);
        
        // ⭐ NEW - Add rating information if rating exists
        if (ratingOpt.isPresent()) {
            Rating rating = ratingOpt.get();
            builder.ratingId(rating.getId())
                   .ratingScore(rating.getRatingScore())
                   .ratingComment(rating.getComment())
                   .ratingCreatedAt(rating.getCreatedAt());
            
            log.debug("📊 Included rating {} (score: {}) for booking {}", 
                     rating.getId(), rating.getRatingScore(), booking.getId());
        }
        
        return builder.build();
    }
}