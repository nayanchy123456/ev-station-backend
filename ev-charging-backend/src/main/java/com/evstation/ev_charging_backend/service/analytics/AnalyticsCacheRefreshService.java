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
            // Clear Host Analytics caches
            clearCache("hostOverview");
            clearCache("hostRevenue");
            clearCache("hostChargers");
            clearCache("hostBookings");
            clearCache("hostUsers");
            
            // Clear User Analytics caches
            clearCache("userOverview");
            clearCache("userSpending");
            clearCache("userBehavior");
            clearCache("userBookings");
            clearCache("userRatings");
            
            // Clear Admin Analytics caches
            clearCache("adminOverview");
            clearCache("adminUsers");
            clearCache("adminHosts");
            clearCache("adminChargers");
            clearCache("adminBookings");
            clearCache("adminRevenue");
            clearCache("adminRatings");
            clearCache("adminPlatformPerformance");
            clearCache("adminTimeAnalytics");
            
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

    /**
     * Clear all admin analytics caches
     * Useful when admin needs fresh platform-wide data
     */
    public void clearAdminCache() {
        log.info("Clearing all admin analytics caches");
        
        clearCache("adminOverview");
        clearCache("adminUsers");
        clearCache("adminHosts");
        clearCache("adminChargers");
        clearCache("adminBookings");
        clearCache("adminRevenue");
        clearCache("adminRatings");
        clearCache("adminPlatformPerformance");
        clearCache("adminTimeAnalytics");
        
        log.info("Admin analytics caches cleared successfully");
    }

    /**
     * Clear cache for a specific date range
     * Useful for targeted cache invalidation
     */
    public void clearCacheForDateRange(String startDate, String endDate) {
        log.info("Clearing cache for date range: {} to {}", startDate, endDate);
        
        // For admin analytics which use date ranges
        clearAdminCache();
        
        log.info("Cache cleared for date range");
    }

    /**
     * Get cache statistics for monitoring
     */
    public String getCacheStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== Analytics Cache Statistics ===\n");
        
        // Host caches
        stats.append("\nHost Analytics:\n");
        appendCacheInfo(stats, "hostOverview");
        appendCacheInfo(stats, "hostRevenue");
        appendCacheInfo(stats, "hostChargers");
        appendCacheInfo(stats, "hostBookings");
        appendCacheInfo(stats, "hostUsers");
        
        // User caches
        stats.append("\nUser Analytics:\n");
        appendCacheInfo(stats, "userOverview");
        appendCacheInfo(stats, "userSpending");
        appendCacheInfo(stats, "userBehavior");
        appendCacheInfo(stats, "userBookings");
        appendCacheInfo(stats, "userRatings");
        
        // Admin caches
        stats.append("\nAdmin Analytics:\n");
        appendCacheInfo(stats, "adminOverview");
        appendCacheInfo(stats, "adminUsers");
        appendCacheInfo(stats, "adminHosts");
        appendCacheInfo(stats, "adminChargers");
        appendCacheInfo(stats, "adminBookings");
        appendCacheInfo(stats, "adminRevenue");
        appendCacheInfo(stats, "adminRatings");
        appendCacheInfo(stats, "adminPlatformPerformance");
        appendCacheInfo(stats, "adminTimeAnalytics");
        
        return stats.toString();
    }

    private void appendCacheInfo(StringBuilder stats, String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            stats.append("  - ").append(cacheName).append(": Active\n");
        } else {
            stats.append("  - ").append(cacheName).append(": Not configured\n");
        }
    }
}