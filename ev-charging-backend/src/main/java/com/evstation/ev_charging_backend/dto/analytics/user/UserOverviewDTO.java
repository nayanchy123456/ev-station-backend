package com.evstation.ev_charging_backend.dto.analytics.user;

import com.evstation.ev_charging_backend.dto.analytics.common.ComparisonData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * User Dashboard Overview - KPI Cards
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOverviewDTO {
    // Current period metrics
    private Long totalBookings;
    private BigDecimal totalSpent;
    private FavoriteChargerInfo favoriteCharger;
    private BigDecimal averageSessionDuration; // in hours
    
    // Period-over-period comparisons
    private ComparisonData spendingComparison;
    private ComparisonData bookingsComparison;
    
    // Additional metrics
    private BigDecimal averageSpendingPerSession;
    private Long completedBookings;
    private BigDecimal totalEnergyConsumed; // kWh
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FavoriteChargerInfo {
        private Long chargerId;
        private String chargerName;
        private Long visits;
    }
}
