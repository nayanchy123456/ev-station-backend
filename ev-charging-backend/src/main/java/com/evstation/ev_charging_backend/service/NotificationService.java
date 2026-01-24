package com.evstation.ev_charging_backend.service;

import com.evstation.ev_charging_backend.dto.NotificationDto;
import com.evstation.ev_charging_backend.entity.Booking;
import com.evstation.ev_charging_backend.enums.NotificationType;

import java.util.List;

public interface NotificationService {
    
    void createNotification(Long userId, NotificationType type, String title, String message, Long bookingId);
    
    List<NotificationDto> getUserNotifications(Long userId);
    
    void markAsRead(Long notificationId, Long userId);
    
    void markAllAsRead(Long userId);
    
    long getUnreadCount(Long userId);
    
    void deleteNotification(Long notificationId, Long userId);
    
    // Helper methods for common notification scenarios
    void notifyUser(Long userId, NotificationType type, Booking booking);
    
    void notifyHost(Long hostId, NotificationType type, Booking booking);
}