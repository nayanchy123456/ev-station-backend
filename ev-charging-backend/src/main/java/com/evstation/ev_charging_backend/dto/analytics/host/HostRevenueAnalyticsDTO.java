package com.evstation.ev_charging_backend.dto.analytics.host;

import com.evstation.ev_charging_backend.dto.analytics.common.ChartDataPoint;
import com.evstation.ev_charging_backend.dto.analytics.common.ComparisonData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Host Revenue Analytics - Revenue charts and breakdowns
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostRevenueAnalyticsDTO {
    // Overall revenue comparison
    private BigDecimal currentPeriodRevenue;
    private BigDecimal previousPeriodRevenue;
    private ComparisonData revenueComparison;
    
    // Timeline data (for line/area chart)
    private List<ChartDataPoint> dailyRevenue;
    
    // Revenue by charger (for bar chart)
    private List<ChargerRevenueDTO> revenueByCharger;
    
    // Period breakdown table
    private PeriodBreakdown todayBreakdown;
    private PeriodBreakdown weekBreakdown;
    private PeriodBreakdown monthBreakdown;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChargerRevenueDTO {
        private Long chargerId;
        private String chargerName;
        private BigDecimal revenue;
        private BigDecimal percentage;
        private Long bookingCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodBreakdown {
        private String period; // "Today", "This Week", "This Month"
        private BigDecimal revenue;
        private Long bookings;
        private BigDecimal changePercentage;
    }
}
