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
public class BookingStatusScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BookingStatusScheduler.class);

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    public BookingStatusScheduler(
            BookingRepository bookingRepository,
            NotificationService notificationService
    ) {
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
    }

    /**
     * Update booking statuses every minute
     * CONFIRMED → ACTIVE when startTime passes
     * ACTIVE → COMPLETED when endTime passes
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void updateBookingStatuses() {
        LocalDateTime now = LocalDateTime.now();

        // Fetch only CONFIRMED and ACTIVE bookings
        List<Booking> bookingsToCheck = bookingRepository.findByStatusIn(
                List.of(BookingStatus.CONFIRMED, BookingStatus.ACTIVE)
        );

        int activatedCount = 0;
        int completedCount = 0;

        for (Booking booking : bookingsToCheck) {
            BookingStatus currentStatus = booking.getStatus();

            // CONFIRMED → ACTIVE
            if (currentStatus == BookingStatus.CONFIRMED && now.isAfter(booking.getStartTime())) {
                booking.setStatus(BookingStatus.ACTIVE);
                bookingRepository.save(booking);

                // Send notification
                notificationService.notifyUser(
                        booking.getUser().getUserId(),
                        NotificationType.BOOKING_ACTIVE,
                        booking
                );
                notificationService.notifyHost(
                        booking.getCharger().getHost().getUserId(),
                        NotificationType.BOOKING_ACTIVE,
                        booking
                );

                activatedCount++;
                logger.info("Activated booking ID: {}", booking.getId());
            }
            // ACTIVE → COMPLETED
            else if (currentStatus == BookingStatus.ACTIVE && now.isAfter(booking.getEndTime())) {
                booking.setStatus(BookingStatus.COMPLETED);
                bookingRepository.save(booking);

                // Send notification
                notificationService.notifyUser(
                        booking.getUser().getUserId(),
                        NotificationType.BOOKING_COMPLETED,
                        booking
                );
                notificationService.notifyHost(
                        booking.getCharger().getHost().getUserId(),
                        NotificationType.BOOKING_COMPLETED,
                        booking
                );

                completedCount++;
                logger.info("Completed booking ID: {}", booking.getId());
            }
        }

        if (activatedCount > 0 || completedCount > 0) {
            logger.info("Status update summary - Activated: {}, Completed: {}", 
                       activatedCount, completedCount);
        }
    }
}