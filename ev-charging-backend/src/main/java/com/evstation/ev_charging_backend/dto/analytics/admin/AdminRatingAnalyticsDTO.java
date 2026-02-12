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
public class AdminRatingAnalyticsDTO {
    
    // Rating Statistics
    private Double overallPlatformRating;
    private Long totalRatingsSubmitted;
    private List<RatingDistribution> ratingDistribution;
    private List<RatingTrend> averageRatingTrend;
    
    // Rating Analysis
    private List<TopRatedCharger> topRatedChargers;
    private List<TopRatedCharger> lowestRatedChargers;
    private List<BrandRating> ratingDistributionByBrand;
    private List<FrequentRater> usersWhoRateMostFrequently;
    
    // Review Insights
    private Long totalReviewsWithComments;
    private List<RecentReview> mostRecentReviews;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingDistribution {
        private Integer stars;
        private Long count;
        private Double percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingTrend {
        private String date;
        private Double averageRating;
        private Long ratingCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopRatedCharger {
        private Long chargerId;
        private String chargerName;
        private String brand;
        private String location;
        private Double averageRating;
        private Long ratingCount;
        private String hostName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrandRating {
        private String brand;
        private Double averageRating;
        private Long ratingCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FrequentRater {
        private Long userId;
        private String userName;
        private Long ratingCount;
        private Double averageRatingGiven;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentReview {
        private Long ratingId;
        private String userName;
        private String chargerName;
        private Integer ratingScore;
        private String comment;
        private LocalDateTime createdAt;
    }
}