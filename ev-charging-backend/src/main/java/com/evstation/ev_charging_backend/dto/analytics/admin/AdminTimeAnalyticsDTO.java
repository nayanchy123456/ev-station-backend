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
public class AdminTimeAnalyticsDTO {
    
    // Temporal Patterns
    private List<DayPattern> busiestDaysOfWeek;
    private List<HourPattern> peakBookingHours;
    private List<SeasonalTrend> seasonalTrends;
    private WeekendVsWeekday weekendVsWeekdayPerformance;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayPattern {
        private String dayOfWeek;
        private Long bookingCount;
        private Double averageUtilization;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourPattern {
        private Integer hour;
        private Long bookingCount;
        private String timeLabel;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeasonalTrend {
        private String period;
        private Long bookingCount;
        private Double averageRating;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeekendVsWeekday {
        private Long weekdayBookings;
        private Long weekendBookings;
        private Double weekdayRevenue;
        private Double weekendRevenue;
    }
}