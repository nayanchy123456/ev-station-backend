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
public class AdminChargerAnalyticsDTO {
    
    // Charger Statistics
    private Long totalChargersRegistered;
    private List<BrandDistribution> chargersByBrand;
    private BigDecimal averagePricePerKwh;
    private List<GeographicData> geographicDistribution;
    
    // Charger Performance
    private List<TopCharger> mostBookedChargers;
    private List<TopCharger> highestRevenueChargers;
    private List<TopCharger> topRatedChargers;
    private List<TopCharger> underutilizedChargers;
    
    // Charger Utilization
    private Double averageBookingRatePerCharger;
    private List<PeakTime> peakBookingTimes;
    private Double chargerAvailabilityRatio;
    private Double totalEnergyConsumedKwh;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrandDistribution {
        private String brand;
        private Long count;
        private Double percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeographicData {
        private String location;
        private Long chargerCount;
        private Long bookingCount;
        private BigDecimal revenue;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCharger {
        private Long chargerId;
        private String name;
        private String brand;
        private String location;
        private Long bookingCount;
        private BigDecimal totalRevenue;
        private Double averageRating;
        private BigDecimal pricePerKwh;
        private String hostName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeakTime {
        private Integer hour;
        private String dayOfWeek;
        private Long bookingCount;
    }
}