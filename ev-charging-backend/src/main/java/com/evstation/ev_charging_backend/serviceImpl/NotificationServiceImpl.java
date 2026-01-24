package com.evstation.ev_charging_backend.serviceImpl;

import com.evstation.ev_charging_backend.dto.NotificationDto;
import com.evstation.ev_charging_backend.entity.Booking;
import com.evstation.ev_charging_backend.entity.Notification;
import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.enums.NotificationType;
import com.evstation.ev_charging_backend.exception.ResourceNotFoundException;
import com.evstation.ev_charging_backend.repository.NotificationRepository;
import com.evstation.ev_charging_backend.repository.UserRepository;
import com.evstation.ev_charging_backend.service.NotificationService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void createNotification(Long userId, NotificationType type, String title, String message, Long bookingId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .booking(bookingId != null ? Booking.builder().id(bookingId).build() : null)
                .read(false)
                .build();

        notificationRepository.save(notification);
    }

    @Override
    public List<NotificationDto> getUserNotifications(Long userId) {
        return notificationRepository.findByUserUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getUserId().equals(userId)) {
            throw new SecurityException("You are not allowed to modify this notification");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> notifications = notificationRepository
                .findByUserUserIdAndReadFalseOrderByCreatedAtDesc(userId);

        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getUserId().equals(userId)) {
            throw new SecurityException("You are not allowed to delete this notification");
        }

        notificationRepository.delete(notification);
    }

    @Override
    @Transactional
    public void notifyUser(Long userId, NotificationType type, Booking booking) {
        String title = generateTitle(type, false);
        String message = generateMessage(type, booking, false);

        createNotification(userId, type, title, message, booking.getId());
    }

    @Override
    @Transactional
    public void notifyHost(Long hostId, NotificationType type, Booking booking) {
        String title = generateTitle(type, true);
        String message = generateMessage(type, booking, true);

        createNotification(hostId, type, title, message, booking.getId());
    }

    private String generateTitle(NotificationType type, boolean isHost) {
        return switch (type) {
            case BOOKING_CONFIRMED -> isHost ? "New Booking Received" : "Booking Confirmed";
            case BOOKING_CANCELLED -> isHost ? "Booking Cancelled" : "Booking Cancelled";
            case BOOKING_ACTIVE -> "Charging Session Started";
            case BOOKING_COMPLETED -> "Charging Session Completed";
            case PAYMENT_SUCCESS -> "Payment Successful";
            case PAYMENT_FAILED -> "Payment Failed";
            case REFUND_PROCESSED -> "Refund Processed";
            case RESERVATION_EXPIRING -> "Reservation Expiring Soon";
            case RESERVATION_EXPIRED -> "Reservation Expired";
        };
    }

    private String generateMessage(NotificationType type, Booking booking, boolean isHost) {
        String chargerName = booking.getCharger().getName();
        String startTime = booking.getStartTime().format(formatter);
        
        // Get user's full name or email as identifier
        User user = booking.getUser();
        String userName = (user.getFirstName() != null && user.getLastName() != null) 
            ? user.getFirstName() + " " + user.getLastName()
            : user.getEmail();

        return switch (type) {
            case BOOKING_CONFIRMED -> isHost 
                ? String.format("%s has booked your charger '%s' for %s", userName, chargerName, startTime)
                : String.format("Your booking for '%s' on %s has been confirmed", chargerName, startTime);
            
            case BOOKING_CANCELLED -> isHost
                ? String.format("%s cancelled their booking for '%s' on %s", userName, chargerName, startTime)
                : String.format("Your booking for '%s' on %s has been cancelled", chargerName, startTime);
            
            case BOOKING_ACTIVE -> 
                String.format("Charging session for '%s' has started", chargerName);
            
            case BOOKING_COMPLETED ->
                String.format("Charging session for '%s' has been completed", chargerName);
            
            case PAYMENT_SUCCESS ->
                String.format("Payment of NPR %.2f for '%s' was successful", 
                    booking.getTotalPrice(), chargerName);
            
            case PAYMENT_FAILED ->
                String.format("Payment for '%s' booking failed. Please try again", chargerName);
            
            case REFUND_PROCESSED ->
                String.format("Refund of NPR %.2f for '%s' booking has been processed", 
                    booking.getTotalPrice(), chargerName);
            
            case RESERVATION_EXPIRING ->
                String.format("Your reservation for '%s' expires in 2 minutes. Complete payment now!", chargerName);
            
            case RESERVATION_EXPIRED ->
                String.format("Your reservation for '%s' has expired. Please create a new booking", chargerName);
        };
    }

    private NotificationDto mapToDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .bookingId(notification.getBooking() != null ? notification.getBooking().getId() : null)
                .read(notification.getRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}