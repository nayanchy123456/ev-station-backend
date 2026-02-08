package com.evstation.ev_charging_backend.dto.analytics.host;

import com.evstation.ev_charging_backend.dto.analytics.common.RatingDistribution;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Host Charger Analytics - Performance metrics for all chargers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostChargerAnalyticsDTO {
    private List<ChargerPerformanceDTO> chargers;
    private List<ChargerPerformanceDTO> topChargersByBookings;
    private List<ChargerPerformanceDTO> topChargersByRevenue;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChargerPerformanceDTO {
        private Long chargerId;
        private String chargerName;
        private String brand;
        private String location;
        private Long totalBookings;
        private BigDecimal revenue;
        private BigDecimal averageRating;
        private Long ratingCount;
        private RatingDistribution ratingDistribution;
        private List<FrequentUserDTO> mostFrequentUsers;
        private String status;
        private LocalDateTime lastBookingDate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FrequentUserDTO {
        private Long userId;
        private String userName;
        private String userEmail;
        private Long bookingCount;
        private BigDecimal totalSpent;
    }
}