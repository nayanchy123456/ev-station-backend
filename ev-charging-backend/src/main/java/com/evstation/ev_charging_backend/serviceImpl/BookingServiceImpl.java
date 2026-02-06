package com.evstation.ev_charging_backend.serviceImpl;

import com.evstation.ev_charging_backend.dto.BookingRequestDto;
import com.evstation.ev_charging_backend.dto.BookingResponseDto;
import com.evstation.ev_charging_backend.entity.Booking;
import com.evstation.ev_charging_backend.entity.Charger;
import com.evstation.ev_charging_backend.entity.Conversation;
import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.enums.BookingStatus;
import com.evstation.ev_charging_backend.exception.BookingConflictException;
import com.evstation.ev_charging_backend.exception.BookingNotFoundException;
import com.evstation.ev_charging_backend.exception.ResourceNotFoundException;
import com.evstation.ev_charging_backend.repository.BookingRepository;
import com.evstation.ev_charging_backend.repository.ChargerRepository;
import com.evstation.ev_charging_backend.repository.ConversationRepository;
import com.evstation.ev_charging_backend.repository.UserRepository;
import com.evstation.ev_charging_backend.service.BookingService;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * ENHANCED BookingServiceImpl with Auto-Conversation Creation
 * 
 * ✅ NEW FEATURES:
 * 1. Automatically creates a conversation between user and host when booking is created
 * 2. Ensures conversation exists for seamless communication
 * 3. Handles edge cases where conversation already exists
 * 
 * This enables users and hosts to communicate directly about their bookings.
 */
@Service
@Slf4j
public class BookingServiceImpl implements BookingService {

    private static final long MIN_DURATION_MINUTES = 30;
    private static final long MAX_DURATION_MINUTES = 8 * 60;
    private static final long MIN_ADVANCE_MINUTES = 15;
    private static final long CANCELLATION_DEADLINE_HOURS = 1;

    private final BookingRepository bookingRepository;
    private final ChargerRepository chargerRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;  // NEW: Added for conversation management

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            ChargerRepository chargerRepository,
            UserRepository userRepository,
            ConversationRepository conversationRepository  // NEW: Added dependency
    ) {
        this.bookingRepository = bookingRepository;
        this.chargerRepository = chargerRepository;
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
    }

    /**
     * Create Booking with Auto-Conversation Creation
     * 
     * ✅ ENHANCED:
     * - Creates a conversation between user and charger host automatically
     * - Logs conversation creation for debugging
     * - Handles cases where conversation already exists (idempotent)
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

        // Validate booking time
        validateTime(dto.getStartTime(), dto.getEndTime());

        // Check for conflicts
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                charger.getId(),
                dto.getStartTime(),
                dto.getEndTime(),
                List.of(BookingStatus.CONFIRMED, BookingStatus.ACTIVE)
        );

        if (!conflicts.isEmpty()) {
            throw new BookingConflictException("Charger already booked for selected time");
        }

        // Create the booking
        Booking booking = Booking.builder()
                .user(user)
                .charger(charger)
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .status(BookingStatus.CONFIRMED)
                .pricePerKwh(charger.getPricePerKwh())
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        log.info("✅ Booking created successfully: {}", savedBooking.getId());

        // ==================== AUTO-CONVERSATION CREATION ====================
        // NEW: Create conversation between user and host (if not exists)
        try {
            createOrGetConversation(userId, host.getUserId());
        } catch (Exception e) {
            // Log but don't fail the booking if conversation creation fails
            log.error("⚠️ Failed to create conversation between user {} and host {}: {}", 
                     userId, host.getUserId(), e.getMessage(), e);
        }
        // ====================================================================

        return mapToResponse(savedBooking);
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
        
        if (booking.getStatus() == BookingStatus.EXPIRED) {
            throw new IllegalStateException("Cannot cancel expired reservation");
        }

        // Allow cancellation of RESERVED and PAYMENT_PENDING without time restrictions
        if (booking.getStatus() == BookingStatus.RESERVED || 
            booking.getStatus() == BookingStatus.PAYMENT_PENDING) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
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
    }

    @Override
    public List<BookingResponseDto> getBookingsByHost(Long hostId) {
        return bookingRepository.findByChargerHostUserIdOrderByStartTimeDesc(hostId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ================= PRIVATE METHODS =================

    /**
     * Create or Get Conversation Between User and Host
     * 
     * This method ensures a conversation exists between the booking user and charger host.
     * It's idempotent - won't create duplicates if conversation already exists.
     * 
     * @param userId ID of the booking user
     * @param hostId ID of the charger host
     * @return Conversation entity
     */
    private Conversation createOrGetConversation(Long userId, Long hostId) {
        // Don't create conversation if user is booking their own charger
        if (userId.equals(hostId)) {
            log.debug("ℹ️ User {} is booking their own charger, skipping conversation creation", userId);
            return null;
        }

        // Check if conversation already exists
        return conversationRepository.findByUsers(userId, hostId)
            .orElseGet(() -> {
                // Normalize: smaller ID first (database constraint)
                Long user1Id = Math.min(userId, hostId);
                Long user2Id = Math.max(userId, hostId);
                
                // Double-check to prevent race conditions
                return conversationRepository.findByUsers(user1Id, user2Id)
                    .orElseGet(() -> {
                        // Create new conversation
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

    /**
     * Helper method to map booking status in real-time
     * Only applies to CONFIRMED bookings - other statuses remain as-is
     */
    private BookingStatus mapStatusRealTime(Booking booking) {
        // Don't modify these statuses
        if (booking.getStatus() == BookingStatus.CANCELLED ||
            booking.getStatus() == BookingStatus.RESERVED ||
            booking.getStatus() == BookingStatus.PAYMENT_PENDING ||
            booking.getStatus() == BookingStatus.EXPIRED) {
            return booking.getStatus();
        }

        LocalDateTime now = LocalDateTime.now();

        // Only CONFIRMED bookings can transition to ACTIVE/COMPLETED
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

    private BookingResponseDto mapToResponse(Booking booking) {
        BigDecimal totalPrice = null;
        if (booking.getTotalEnergyKwh() != null) {
            totalPrice = booking.getPricePerKwh()
                    .multiply(BigDecimal.valueOf(booking.getTotalEnergyKwh()));
        } else if (booking.getTotalPrice() != null) {
            totalPrice = booking.getTotalPrice();
        }

        return BookingResponseDto.builder()
                .bookingId(booking.getId())
                .userId(booking.getUser().getUserId())
                .chargerId(booking.getCharger().getId())
                .chargerName(booking.getCharger().getName())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .status(mapStatusRealTime(booking))
                .reservedUntil(booking.getReservedUntil())
                .pricePerKwh(booking.getPricePerKwh())
                .totalPrice(totalPrice)
                .build();
    }
}