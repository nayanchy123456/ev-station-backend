package com.evstation.ev_charging_backend.dto.analytics.host;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Host User Analytics - User behavior and top customers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostUserAnalyticsDTO {
    private Long totalActiveUsers;
    private Long newUsers;
    private Long returningUsers;
    private List<TopUserDTO> topFrequentUsers;
    private List<UserChargerAffinityDTO> userChargerAffinity;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopUserDTO {
        private Long userId;
        private String userName;
        private String userEmail;
        private Long totalBookings;
        private BigDecimal totalSpent;
        private Long favoriteChargerId;
        private String favoriteChargerName;
        private Long favoriteChargerVisits;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserChargerAffinityDTO {
        private Long userId;
        private String userName;
        private Long chargerId;
        private String chargerName;
        private Long bookingCount;
        private BigDecimal totalSpent;
    }
}