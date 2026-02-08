package com.evstation.ev_charging_backend.dto.analytics.host;

import com.evstation.ev_charging_backend.dto.analytics.common.DistributionData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Host Booking Analytics - Booking status and trends
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostBookingAnalyticsDTO {
    private Long totalBookings;
    private Long completedBookings;
    private Long cancelledBookings;
    private Long expiredBookings;
    private Long activeBookings;
    private List<DistributionData> bookingStatusDistribution;
    private ExpiredBookingsAnalysis expiredAnalysis;
    private BigDecimal completionRate;
    private BigDecimal cancellationRate;
    private BigDecimal expirationRate;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpiredBookingsAnalysis {
        private Long totalExpiredCount;
        private BigDecimal totalLostRevenue;
        private PeriodExpiredData thisWeek;
        private PeriodExpiredData thisMonth;
        private PeriodExpiredData allTime;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodExpiredData {
        private String period;
        private Long expiredCount;
        private BigDecimal lostRevenue;
    }
}