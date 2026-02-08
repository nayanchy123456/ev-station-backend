package com.evstation.ev_charging_backend.service.analytics;

import com.evstation.ev_charging_backend.dto.analytics.common.AnalyticsPeriod;
import com.evstation.ev_charging_backend.dto.analytics.user.*;

/**
 * Service interface for User Analytics
 */
public interface UserAnalyticsService {
    
    /**
     * Get overview/KPI metrics for user dashboard
     */
    UserOverviewDTO getOverview(Long userId, AnalyticsPeriod period);
    
    /**
     * Get spending analytics for user
     */
    UserSpendingAnalyticsDTO getSpendingAnalytics(Long userId, AnalyticsPeriod period);
    
    /**
     * Get charging behavior analytics
     */
    UserChargingBehaviorDTO getChargingBehavior(Long userId, AnalyticsPeriod period);
    
    /**
     * Get booking analytics
     */
    UserBookingAnalyticsDTO getBookingAnalytics(Long userId, AnalyticsPeriod period);
    
    /**
     * Get rating analytics (ALL_TIME only - ratings don't change often)
     */
    UserRatingAnalyticsDTO getRatingAnalytics(Long userId);
}