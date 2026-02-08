package com.evstation.ev_charging_backend.dto.analytics.host;

import com.evstation.ev_charging_backend.dto.analytics.common.ComparisonData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Host Dashboard Overview - KPI Cards
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostOverviewDTO {
    // Current period metrics
    private Long totalChargers;
    private Long activeUsers; // Unique users who booked in this period
    private BigDecimal totalRevenue;
    private BigDecimal averageRating;
    
    // Period-over-period comparisons
    private ComparisonData revenueComparison;
    private ComparisonData activeUsersComparison;
    private ComparisonData bookingsComparison;
    
    // Additional metrics
    private Long totalBookings;
    private Long completedBookings;
    private BigDecimal completionRate; // percentage
}
