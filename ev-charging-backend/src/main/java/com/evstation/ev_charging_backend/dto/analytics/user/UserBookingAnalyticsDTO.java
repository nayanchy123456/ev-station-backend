package com.evstation.ev_charging_backend.dto.analytics.user;

import com.evstation.ev_charging_backend.dto.analytics.common.DistributionData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * User Booking Analytics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBookingAnalyticsDTO {
    // Overall booking counts
    private Long totalBookings;
    private Long completedBookings;
    private Long cancelledBookings;
    private Long upcomingBookings;
    
    // Status distribution (for pie/donut chart)
    private List<DistributionData> bookingStatusDistribution;
    
    // Recent bookings
    private List<RecentBookingDTO> recentBookings;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentBookingDTO {
        private Long bookingId;
        private Long chargerId;
        private String chargerName;
        private LocalDateTime date;
        private BigDecimal duration; // hours
        private BigDecimal cost;
        private String status;
        private BigDecimal energyConsumed; // kWh
    }
}
