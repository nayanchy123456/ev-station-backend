package com.evstation.ev_charging_backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache Configuration for Analytics
 * 
 * Uses simple in-memory caching with automatic eviction
 * For production, consider using Redis or Caffeine for better performance
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "hostOverview",
            "hostRevenue",
            "hostChargers",
            "hostBookings",
            "hostUsers",
            "userOverview",
            "userSpending",
            "userBehavior",
            "userBookings",
            "userRatings"
        );
    }
}
