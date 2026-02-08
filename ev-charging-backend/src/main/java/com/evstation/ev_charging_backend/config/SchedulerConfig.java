package com.evstation.ev_charging_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Scheduler Configuration
 * Enables scheduled tasks for cache refresh
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // Scheduling is now enabled
}
