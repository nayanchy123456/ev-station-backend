package com.evstation.ev_charging_backend.scheduler;

import com.evstation.ev_charging_backend.entity.Booking;
import com.evstation.ev_charging_backend.enums.BookingStatus;
import com.evstation.ev_charging_backend.enums.NotificationType;
import com.evstation.ev_charging_backend.repository.BookingRepository;
import com.evstation.ev_charging_backend.service.NotificationService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReservationExpiryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReservationExpiryScheduler.class);

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    public ReservationExpiryScheduler(
            BookingRepository bookingRepository,
            NotificationService notificationService
    ) {
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
    }

    /**
     * Check for expired reservations every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void checkExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();

        // Find RESERVED bookings past their reservedUntil time
        List<Booking> expiredReservations = bookingRepository
                .findByStatusAndReservedUntilBefore(BookingStatus.RESERVED, now);

        if (!expiredReservations.isEmpty()) {
            logger.info("Found {} expired reservations", expiredReservations.size());

            for (Booking booking : expiredReservations) {
                booking.setStatus(BookingStatus.EXPIRED);
                bookingRepository.save(booking);

                // Send notification to user
                notificationService.notifyUser(
                        booking.getUser().getUserId(),
                        NotificationType.RESERVATION_EXPIRED,
                        booking
                );

                logger.info("Expired reservation ID: {}", booking.getId());
            }
        }
    }

    /**
     * Send warning 2 minutes before expiry (optional feature)
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void sendExpiryWarnings() {
        LocalDateTime twoMinutesFromNow = LocalDateTime.now().plusMinutes(2);
        LocalDateTime oneMinuteFromNow = LocalDateTime.now().plusMinutes(1);

        // Find reservations expiring in next 1-2 minutes
        List<Booking> expiringReservations = bookingRepository.findByStatusIn(
                List.of(BookingStatus.RESERVED, BookingStatus.PAYMENT_PENDING)
        ).stream()
         .filter(b -> b.getReservedUntil() != null &&
                     b.getReservedUntil().isAfter(oneMinuteFromNow) &&
                     b.getReservedUntil().isBefore(twoMinutesFromNow))
         .toList();

        for (Booking booking : expiringReservations) {
            // Check if warning was already sent (you can add a field to track this)
            notificationService.notifyUser(
                    booking.getUser().getUserId(),
                    NotificationType.RESERVATION_EXPIRING,
                    booking
            );

            logger.info("Sent expiry warning for reservation ID: {}", booking.getId());
        }
    }
}