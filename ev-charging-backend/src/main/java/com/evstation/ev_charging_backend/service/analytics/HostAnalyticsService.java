package com.evstation.ev_charging_backend.service.analytics;

import com.evstation.ev_charging_backend.dto.analytics.common.AnalyticsPeriod;
import com.evstation.ev_charging_backend.dto.analytics.host.*;

/**
 * Service interface for Host Analytics
 */
public interface HostAnalyticsService {
    
    /**
     * Get overview/KPI metrics for host dashboard
     */
    HostOverviewDTO getOverview(Long hostId, AnalyticsPeriod period);
    
    /**
     * Get revenue analytics for host
     */
    HostRevenueAnalyticsDTO getRevenueAnalytics(Long hostId, AnalyticsPeriod period);
    
    /**
     * Get charger performance analytics
     */
    HostChargerAnalyticsDTO getChargerAnalytics(Long hostId, AnalyticsPeriod period);
    
    /**
     * Get booking analytics
     */
    HostBookingAnalyticsDTO getBookingAnalytics(Long hostId, AnalyticsPeriod period);
    
    /**
     * Get user behavior analytics
     */
    HostUserAnalyticsDTO getUserAnalytics(Long hostId, AnalyticsPeriod period);
}