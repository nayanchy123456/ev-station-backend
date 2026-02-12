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
public class AdminBookingAnalyticsDTO {
    
    // Booking Statistics
    private Long totalBookings;
    private List<StatusDistribution> bookingsByStatus;
    private Double bookingCompletionRate;
    private Double averageBookingDurationHours;
    private Double cancellationRate;
    
    // Booking Trends
    private List<BookingTrend> dailyBookingTrend;
    private List<BookingTrend> weeklyBookingComparison;
    private List<BookingTrend> monthlyBookingComparison;
    private List<PeakHour> peakBookingHours;
    private List<DayFrequency> bookingFrequencyByDay;
    
    // Booking Behavior
    private Double averageLeadTimeHours;
    private List<DurationDistribution> popularBookingDurations;
    private Double reservationExpiryRate;
    private Double repeatBookingRate;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusDistribution {
        private String status;
        private Long count;
        private Double percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingTrend {
        private String date;
        private Long count;
        private String period;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeakHour {
        private Integer hour;
        private Long bookingCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayFrequency {
        private String dayOfWeek;
        private Long count;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DurationDistribution {
        private String durationRange;
        private Long count;
    }
}