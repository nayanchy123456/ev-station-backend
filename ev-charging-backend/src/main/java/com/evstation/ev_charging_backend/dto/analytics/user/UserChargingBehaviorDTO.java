package com.evstation.ev_charging_backend.dto.analytics.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * User Charging Behavior Analytics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserChargingBehaviorDTO {
    // Most visited chargers
    private List<VisitedChargerDTO> mostVisitedChargers;
    
    // Charging patterns
    private ChargingPatternsDTO chargingPatterns;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisitedChargerDTO {
        private Long chargerId;
        private String chargerName;
        private String brand;
        private String location;
        private Long visits;
        private BigDecimal totalSpent;
        private BigDecimal averageRatingGiven;
        private LocalDateTime lastVisit;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChargingPatternsDTO {
        private BigDecimal averageSessionDuration; // hours
        private BigDecimal totalEnergyConsumed; // kWh
        private String peakChargingDay; // day of week
        private Integer peakChargingHour; // 0-23
        private BigDecimal averageEnergyPerSession; // kWh
    }
}
