package com.evstation.ev_charging_backend.dto.analytics.user;

import com.evstation.ev_charging_backend.dto.analytics.common.ChartDataPoint;
import com.evstation.ev_charging_backend.dto.analytics.common.ComparisonData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * User Spending Analytics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSpendingAnalyticsDTO {
    // Overall spending comparison
    private BigDecimal currentPeriodSpending;
    private BigDecimal previousPeriodSpending;
    private ComparisonData spendingComparison;
    
    // Timeline data (for area chart)
    private List<ChartDataPoint> dailySpending;
    
    // Spending by charger (for pie chart)
    private List<ChargerSpendingDTO> spendingByCharger;
    
    // Metrics
    private BigDecimal averageSpendingPerSession;
    private Long totalSessions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChargerSpendingDTO {
        private Long chargerId;
        private String chargerName;
        private BigDecimal spent;
        private BigDecimal percentage;
        private Long sessionCount;
    }
}
