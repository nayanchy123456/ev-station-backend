package com.evstation.ev_charging_backend.dto.analytics.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOverviewDTO {
    
    // Key Metrics
    private Long totalUsers;
    private Long totalHosts;
    private Long pendingHostApprovals;
    private Long totalChargers;
    private Long activeBookings;
    private BigDecimal totalRevenue;
    private Double averageRating;
    
    // Quick Stats
    private Long todaysBookings;
    private BigDecimal thisWeekRevenue;
    private Long newUsersLast30Days;
    private Double completionRate;
    
    // Trend Data
    private List<TrendDataPoint> revenueTrend;
    private List<TrendDataPoint> bookingTrend;
    private List<TrendDataPoint> userGrowthTrend;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDataPoint {
        private String date;
        private BigDecimal value;
        private Long count;
    }
}