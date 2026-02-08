package com.evstation.ev_charging_backend.controller.analytics;

import com.evstation.ev_charging_backend.dto.analytics.common.AnalyticsPeriod;
import com.evstation.ev_charging_backend.dto.analytics.host.*;
import com.evstation.ev_charging_backend.security.CustomUserDetails;
import com.evstation.ev_charging_backend.service.analytics.HostAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Host Analytics
 * 
 * All endpoints require HOST role authentication
 */
@RestController
@RequestMapping("/api/analytics/host")
@RequiredArgsConstructor
@Slf4j
public class HostAnalyticsController {

    private final HostAnalyticsService hostAnalyticsService;

    /**
     * Get Host Overview/KPI Metrics
     * 
     * @param period - LAST_7_DAYS, LAST_30_DAYS, or ALL_TIME
     * @param authentication - authenticated user info
     * @return HostOverviewDTO with KPI cards data
     * 
     * Example: GET /api/analytics/host/overview?period=LAST_7_DAYS
     */
    @GetMapping("/overview")
    public ResponseEntity<HostOverviewDTO> getOverview(
            @RequestParam(defaultValue = "LAST_7_DAYS") String period,
            Authentication authentication) {
        
        log.info("Getting host overview for period: {}", period);
        
        Long hostId = getUserIdFromAuth(authentication);
        AnalyticsPeriod analyticsPeriod = AnalyticsPeriod.fromString(period);
        
        HostOverviewDTO overview = hostAnalyticsService.getOverview(hostId, analyticsPeriod);
        return ResponseEntity.ok(overview);
    }

    /**
     * Get Host Revenue Analytics
     * 
     * @param period - time period filter
     * @param authentication - authenticated user
     * @return HostRevenueAnalyticsDTO with revenue charts and breakdowns
     * 
     * Example: GET /api/analytics/host/revenue?period=LAST_30_DAYS
     */
    @GetMapping("/revenue")
    public ResponseEntity<HostRevenueAnalyticsDTO> getRevenueAnalytics(
            @RequestParam(defaultValue = "LAST_7_DAYS") String period,
            Authentication authentication) {
        
        log.info("Getting revenue analytics for period: {}", period);
        
        Long hostId = getUserIdFromAuth(authentication);
        AnalyticsPeriod analyticsPeriod = AnalyticsPeriod.fromString(period);
        
        HostRevenueAnalyticsDTO revenueAnalytics = hostAnalyticsService.getRevenueAnalytics(hostId, analyticsPeriod);
        return ResponseEntity.ok(revenueAnalytics);
    }

    /**
     * Get Host Charger Performance Analytics
     * 
     * @param period - time period filter
     * @param authentication - authenticated user
     * @return HostChargerAnalyticsDTO with charger performance data
     * 
     * Example: GET /api/analytics/host/chargers?period=ALL_TIME
     */
    @GetMapping("/chargers")
    public ResponseEntity<HostChargerAnalyticsDTO> getChargerAnalytics(
            @RequestParam(defaultValue = "LAST_7_DAYS") String period,
            Authentication authentication) {
        
        log.info("Getting charger analytics for period: {}", period);
        
        Long hostId = getUserIdFromAuth(authentication);
        AnalyticsPeriod analyticsPeriod = AnalyticsPeriod.fromString(period);
        
        HostChargerAnalyticsDTO chargerAnalytics = hostAnalyticsService.getChargerAnalytics(hostId, analyticsPeriod);
        return ResponseEntity.ok(chargerAnalytics);
    }

    /**
     * Get Host Booking Analytics
     * 
     * @param period - time period filter
     * @param authentication - authenticated user
     * @return HostBookingAnalyticsDTO with booking status and trends
     * 
     * Example: GET /api/analytics/host/bookings?period=LAST_30_DAYS
     */
    @GetMapping("/bookings")
    public ResponseEntity<HostBookingAnalyticsDTO> getBookingAnalytics(
            @RequestParam(defaultValue = "LAST_7_DAYS") String period,
            Authentication authentication) {
        
        log.info("Getting booking analytics for period: {}", period);
        
        Long hostId = getUserIdFromAuth(authentication);
        AnalyticsPeriod analyticsPeriod = AnalyticsPeriod.fromString(period);
        
        HostBookingAnalyticsDTO bookingAnalytics = hostAnalyticsService.getBookingAnalytics(hostId, analyticsPeriod);
        return ResponseEntity.ok(bookingAnalytics);
    }

    /**
     * Get Host User Behavior Analytics
     * 
     * @param period - time period filter
     * @param authentication - authenticated user
     * @return HostUserAnalyticsDTO with user behavior data
     * 
     * Example: GET /api/analytics/host/users?period=LAST_30_DAYS
     */
    @GetMapping("/users")
    public ResponseEntity<HostUserAnalyticsDTO> getUserAnalytics(
            @RequestParam(defaultValue = "LAST_7_DAYS") String period,
            Authentication authentication) {
        
        log.info("Getting user analytics for period: {}", period);
        
        Long hostId = getUserIdFromAuth(authentication);
        AnalyticsPeriod analyticsPeriod = AnalyticsPeriod.fromString(period);
        
        HostUserAnalyticsDTO userAnalytics = hostAnalyticsService.getUserAnalytics(hostId, analyticsPeriod);
        return ResponseEntity.ok(userAnalytics);
    }

    /**
     * Helper method to extract user ID from authentication
     * FIXED VERSION - Properly extracts userId from JWT token
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) principal;
            Long userId = userDetails.getUserId();
            
            if (userId == null) {
                throw new RuntimeException("User ID not found in authentication");
            }
            
            log.debug("Extracted userId: {} from authentication", userId);
            return userId;
        }
        
        throw new RuntimeException("Invalid authentication principal type: " + principal.getClass().getName());
    }
}