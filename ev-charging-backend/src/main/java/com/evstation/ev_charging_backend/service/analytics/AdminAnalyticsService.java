package com.evstation.ev_charging_backend.service.analytics;

import com.evstation.ev_charging_backend.dto.analytics.admin.*;
import java.time.LocalDate;

public interface AdminAnalyticsService {
    
    // Overview Analytics
    AdminOverviewDTO getOverviewAnalytics(LocalDate startDate, LocalDate endDate);
    
    // User Analytics
    AdminUserAnalyticsDTO getUserAnalytics(LocalDate startDate, LocalDate endDate);
    
    // Host Analytics
    AdminHostAnalyticsDTO getHostAnalytics(LocalDate startDate, LocalDate endDate);
    
    // Charger Analytics
    AdminChargerAnalyticsDTO getChargerAnalytics(LocalDate startDate, LocalDate endDate);
    
    // Booking Analytics
    AdminBookingAnalyticsDTO getBookingAnalytics(LocalDate startDate, LocalDate endDate);
    
    // Revenue Analytics
    AdminRevenueAnalyticsDTO getRevenueAnalytics(LocalDate startDate, LocalDate endDate);
    
    // Rating Analytics
    AdminRatingAnalyticsDTO getRatingAnalytics(LocalDate startDate, LocalDate endDate);
    
    // Platform Performance Analytics
    AdminPlatformPerformanceDTO getPlatformPerformanceAnalytics(LocalDate startDate, LocalDate endDate);
    
    // Time-based Analytics
    AdminTimeAnalyticsDTO getTimeAnalytics(LocalDate startDate, LocalDate endDate);
}