package com.evstation.ev_charging_backend.dto.analytics.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserAnalyticsDTO {
    
    // User Statistics
    private Long totalRegisteredUsers;
    private Long activeUsers;
    private Double userGrowthRate;
    private List<UserRegistrationTrend> userRegistrationTimeline;
    
    // User Behavior
    private List<TopUser> topActiveUsers;
    private Double averageBookingsPerUser;
    private Double userRetentionRate;
    
    // User Segmentation
    private UserSegmentation usersByRole;
    private List<ActivityHeatmap> activityHeatmap;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRegistrationTrend {
        private String date;
        private Long count;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopUser {
        private Long userId;
        private String name;
        private String email;
        private Long bookingCount;
        private LocalDateTime lastActive;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSegmentation {
        private Long totalUsers;
        private Long hosts;
        private Long regularUsers;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityHeatmap {
        private String dayOfWeek;
        private Integer hour;
        private Long activityCount;
    }
}