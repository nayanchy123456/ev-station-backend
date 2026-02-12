package com.evstation.ev_charging_backend.dto.analytics.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPlatformPerformanceDTO {
    
    // System Health
    private Long totalTransactionsProcessed;
    private Long totalNotificationsSent;
    
    // Engagement Metrics
    private Long dailyActiveUsers;
    private Long weeklyActiveUsers;
    private Long monthlyActiveUsers;
    private Double notificationDeliveryRate;
    
    // Growth Metrics
    private GrowthMetrics monthOverMonthGrowth;
    private GrowthMetrics yearOverYearGrowth;
    private Double platformAdoptionRate;
    private Double userToBookingConversionRate;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrowthMetrics {
        private Double userGrowth;
        private Double hostGrowth;
        private Double bookingGrowth;
        private Double revenueGrowth;
    }
}