package com.evstation.ev_charging_backend.service.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Service for scheduled cache refresh
 * Automatically clears analytics cache every 2 minutes for real-time updates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsCacheRefreshService {

    private final CacheManager cacheManager;

    /**
     * Refresh analytics cache every 2 minutes (120,000 milliseconds)
     * This ensures users get fresh data without overwhelming the database
     * 
     * Updated from 5 minutes to 2 minutes for more real-time analytics updates
     * 
     * For production, consider:
     * - Selective cache eviction (only evict changed data)
     * - Different refresh rates for different analytics (e.g., revenue more frequent)
     * - Event-driven cache invalidation (on booking/payment completion)
     */
    @Scheduled(fixedRate = 120000) // 2 minutes (120,000 milliseconds)
    public void refreshAnalyticsCache() {
        log.info("Starting scheduled analytics cache refresh (2-minute interval)");
        
        try {
            // Clear all analytics caches
            clearCache("hostOverview");
            clearCache("hostRevenue");
            clearCache("hostChargers");
            clearCache("hostBookings");
            clearCache("hostUsers");
            clearCache("userOverview");
            clearCache("userSpending");
            clearCache("userBehavior");
            clearCache("userBookings");
            clearCache("userRatings");
            
            log.info("Analytics cache refresh completed successfully");
        } catch (Exception e) {
            log.error("Error during cache refresh", e);
        }
    }

    /**
     * Clear a specific cache by name
     */
    private void clearCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.debug("Cleared cache: {}", cacheName);
        }
    }

    /**
     * Manual cache refresh endpoint (can be exposed via admin API)
     * Useful for immediate updates after data changes
     */
    public void manualRefresh() {
        log.info("Manual cache refresh triggered");
        refreshAnalyticsCache();
    }

    /**
     * Clear cache for a specific user
     * Useful when user data changes significantly
     */
    public void clearUserCache(Long userId) {
        log.info("Clearing cache for user: {}", userId);
        
        var userOverview = cacheManager.getCache("userOverview");
        var userSpending = cacheManager.getCache("userSpending");
        var userBehavior = cacheManager.getCache("userBehavior");
        var userBookings = cacheManager.getCache("userBookings");
        var userRatings = cacheManager.getCache("userRatings");
        
        // Evict all entries for this user (all periods)
        if (userOverview != null) userOverview.clear(); // In simple cache, clear all
        if (userSpending != null) userSpending.clear();
        if (userBehavior != null) userBehavior.clear();
        if (userBookings != null) userBookings.clear();
        if (userRatings != null) userRatings.clear();
    }

    /**
     * Clear cache for a specific host
     * Useful when host data changes significantly
     */
    public void clearHostCache(Long hostId) {
        log.info("Clearing cache for host: {}", hostId);
        
        var hostOverview = cacheManager.getCache("hostOverview");
        var hostRevenue = cacheManager.getCache("hostRevenue");
        var hostChargers = cacheManager.getCache("hostChargers");
        var hostBookings = cacheManager.getCache("hostBookings");
        var hostUsers = cacheManager.getCache("hostUsers");
        
        // Evict all entries for this host (all periods)
        if (hostOverview != null) hostOverview.clear();
        if (hostRevenue != null) hostRevenue.clear();
        if (hostChargers != null) hostChargers.clear();
        if (hostBookings != null) hostBookings.clear();
        if (hostUsers != null) hostUsers.clear();
    }
}