package com.evstation.ev_charging_backend.controller.analytics;

import com.evstation.ev_charging_backend.dto.analytics.admin.*;
import com.evstation.ev_charging_backend.service.analytics.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST Controller for Admin Analytics
 * Provides comprehensive platform analytics for administrators
 */
@RestController
@RequestMapping("/api/analytics/admin")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://localhost:8080"}, allowCredentials = "true")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    /**
     * Get overview/dashboard analytics for admin
     * @param startDate Start date for analytics period
     * @param endDate End date for analytics period
     * @return Overview analytics DTO
     */
    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminOverviewDTO> getOverviewAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("GET /api/analytics/admin/overview - Date range: {} to {}", startDate, endDate);
        
        try {
            AdminOverviewDTO analytics = adminAnalyticsService.getOverviewAnalytics(startDate, endDate);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error fetching admin overview analytics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get user analytics for admin
     * @param startDate Start date for analytics period
     * @param endDate End date for analytics period
     * @return User analytics DTO
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserAnalyticsDTO> getUserAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("GET /api/analytics/admin/users - Date range: {} to {}", startDate, endDate);
        
        try {
            AdminUserAnalyticsDTO analytics = adminAnalyticsService.getUserAnalytics(startDate, endDate);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error fetching admin user analytics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get host analytics for admin
     * @param startDate Start date for analytics period
     * @param endDate End date for analytics period
     * @return Host analytics DTO
     */
    @GetMapping("/hosts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminHostAnalyticsDTO> getHostAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("GET /api/analytics/admin/hosts - Date range: {} to {}", startDate, endDate);
        
        try {
            AdminHostAnalyticsDTO analytics = adminAnalyticsService.getHostAnalytics(startDate, endDate);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error fetching admin host analytics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get charger analytics for admin
     * @param startDate Start date for analytics period
     * @param endDate End date for analytics period
     * @return Charger analytics DTO
     */
    @GetMapping("/chargers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminChargerAnalyticsDTO> getChargerAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("GET /api/analytics/admin/chargers - Date range: {} to {}", startDate, endDate);
        
        try {
            AdminChargerAnalyticsDTO analytics = adminAnalyticsService.getChargerAnalytics(startDate, endDate);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error fetching admin charger analytics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get booking analytics for admin
     * @param startDate Start date for analytics period
     * @param endDate End date for analytics period
     * @return Booking analytics DTO
     */
    @GetMapping("/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminBookingAnalyticsDTO> getBookingAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("GET /api/analytics/admin/bookings - Date range: {} to {}", startDate, endDate);
        
        try {
            AdminBookingAnalyticsDTO analytics = adminAnalyticsService.getBookingAnalytics(startDate, endDate);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error fetching admin booking analytics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get revenue analytics for admin
     * @param startDate Start date for analytics period
     * @param endDate End date for analytics period
     * @return Revenue analytics DTO
     */
    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminRevenueAnalyticsDTO> getRevenueAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("GET /api/analytics/admin/revenue - Date range: {} to {}", startDate, endDate);
        
        try {
            AdminRevenueAnalyticsDTO analytics = adminAnalyticsService.getRevenueAnalytics(startDate, endDate);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error fetching admin revenue analytics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get rating analytics for admin
     * @param startDate Start date for analytics period
     * @param endDate End date for analytics period
     * @return Rating analytics DTO
     */
    @GetMapping("/ratings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminRatingAnalyticsDTO> getRatingAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("GET /api/analytics/admin/ratings - Date range: {} to {}", startDate, endDate);
        
        try {
            AdminRatingAnalyticsDTO analytics = adminAnalyticsService.getRatingAnalytics(startDate, endDate);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error fetching admin rating analytics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get platform performance analytics for admin
     * @param startDate Start date for analytics period
     * @param endDate End date for analytics period
     * @return Platform performance analytics DTO
     */
    @GetMapping("/platform-performance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminPlatformPerformanceDTO> getPlatformPerformanceAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("GET /api/analytics/admin/platform-performance - Date range: {} to {}", startDate, endDate);
        
        try {
            AdminPlatformPerformanceDTO analytics = adminAnalyticsService.getPlatformPerformanceAnalytics(startDate, endDate);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error fetching admin platform performance analytics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get time-based analytics for admin
     * @param startDate Start date for analytics period
     * @param endDate End date for analytics period
     * @return Time analytics DTO
     */
    @GetMapping("/time-analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminTimeAnalyticsDTO> getTimeAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("GET /api/analytics/admin/time-analytics - Date range: {} to {}", startDate, endDate);
        
        try {
            AdminTimeAnalyticsDTO analytics = adminAnalyticsService.getTimeAnalytics(startDate, endDate);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error fetching admin time analytics", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}