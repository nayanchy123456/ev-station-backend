package com.evstation.ev_charging_backend.controller.analytics;

import com.evstation.ev_charging_backend.dto.analytics.common.AnalyticsPeriod;
import com.evstation.ev_charging_backend.dto.analytics.user.*;
import com.evstation.ev_charging_backend.security.CustomUserDetails;
import com.evstation.ev_charging_backend.service.analytics.UserAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for User Analytics
 * 
 * All endpoints require USER role authentication
 */
@RestController
@RequestMapping("/api/analytics/user")
@RequiredArgsConstructor
@Slf4j
public class UserAnalyticsController {

    private final UserAnalyticsService userAnalyticsService;

    /**
     * Get User Overview/KPI Metrics
     * 
     * @param period - LAST_7_DAYS, LAST_30_DAYS, or ALL_TIME
     * @param authentication - authenticated user info
     * @return UserOverviewDTO with KPI cards data
     * 
     * Example: GET /api/analytics/user/overview?period=LAST_7_DAYS
     */
    @GetMapping("/overview")
    public ResponseEntity<UserOverviewDTO> getOverview(
            @RequestParam(defaultValue = "LAST_7_DAYS") String period,
            Authentication authentication) {
        
        log.info("Getting user overview for period: {}", period);
        
        Long userId = getUserIdFromAuth(authentication);
        AnalyticsPeriod analyticsPeriod = AnalyticsPeriod.fromString(period);
        
        UserOverviewDTO overview = userAnalyticsService.getOverview(userId, analyticsPeriod);
        return ResponseEntity.ok(overview);
    }

    /**
     * Get User Spending Analytics
     * 
     * @param period - time period filter
     * @param authentication - authenticated user
     * @return UserSpendingAnalyticsDTO with spending charts and breakdowns
     * 
     * Example: GET /api/analytics/user/spending?period=LAST_30_DAYS
     */
    @GetMapping("/spending")
    public ResponseEntity<UserSpendingAnalyticsDTO> getSpendingAnalytics(
            @RequestParam(defaultValue = "LAST_7_DAYS") String period,
            Authentication authentication) {
        
        log.info("Getting spending analytics for period: {}", period);
        
        Long userId = getUserIdFromAuth(authentication);
        AnalyticsPeriod analyticsPeriod = AnalyticsPeriod.fromString(period);
        
        UserSpendingAnalyticsDTO spendingAnalytics = userAnalyticsService.getSpendingAnalytics(userId, analyticsPeriod);
        return ResponseEntity.ok(spendingAnalytics);
    }

    /**
     * Get User Charging Behavior Analytics
     * 
     * @param period - time period filter
     * @param authentication - authenticated user
     * @return UserChargingBehaviorDTO with charging patterns
     * 
     * Example: GET /api/analytics/user/charging-behavior?period=ALL_TIME
     */
    @GetMapping("/charging-behavior")
    public ResponseEntity<UserChargingBehaviorDTO> getChargingBehavior(
            @RequestParam(defaultValue = "LAST_7_DAYS") String period,
            Authentication authentication) {
        
        log.info("Getting charging behavior for period: {}", period);
        
        Long userId = getUserIdFromAuth(authentication);
        AnalyticsPeriod analyticsPeriod = AnalyticsPeriod.fromString(period);
        
        UserChargingBehaviorDTO behaviorAnalytics = userAnalyticsService.getChargingBehavior(userId, analyticsPeriod);
        return ResponseEntity.ok(behaviorAnalytics);
    }

    /**
     * Get User Booking Analytics
     * 
     * @param period - time period filter
     * @param authentication - authenticated user
     * @return UserBookingAnalyticsDTO with booking history and status
     * 
     * Example: GET /api/analytics/user/bookings?period=LAST_30_DAYS
     */
    @GetMapping("/bookings")
    public ResponseEntity<UserBookingAnalyticsDTO> getBookingAnalytics(
            @RequestParam(defaultValue = "LAST_7_DAYS") String period,
            Authentication authentication) {
        
        log.info("Getting booking analytics for period: {}", period);
        
        Long userId = getUserIdFromAuth(authentication);
        AnalyticsPeriod analyticsPeriod = AnalyticsPeriod.fromString(period);
        
        UserBookingAnalyticsDTO bookingAnalytics = userAnalyticsService.getBookingAnalytics(userId, analyticsPeriod);
        return ResponseEntity.ok(bookingAnalytics);
    }

    /**
     * Get User Rating Analytics
     * Note: This always returns ALL_TIME data as ratings don't change frequently
     * 
     * @param authentication - authenticated user
     * @return UserRatingAnalyticsDTO with rating history
     * 
     * Example: GET /api/analytics/user/ratings
     */
    @GetMapping("/ratings")
    public ResponseEntity<UserRatingAnalyticsDTO> getRatingAnalytics(
            Authentication authentication) {
        
        log.info("Getting rating analytics (all time)");
        
        Long userId = getUserIdFromAuth(authentication);
        
        UserRatingAnalyticsDTO ratingAnalytics = userAnalyticsService.getRatingAnalytics(userId);
        return ResponseEntity.ok(ratingAnalytics);
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