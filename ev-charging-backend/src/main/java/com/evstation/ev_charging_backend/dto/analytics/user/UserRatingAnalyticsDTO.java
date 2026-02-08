package com.evstation.ev_charging_backend.dto.analytics.user;

import com.evstation.ev_charging_backend.dto.analytics.common.RatingDistribution;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * User Rating Analytics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRatingAnalyticsDTO {
    // Overall rating metrics
    private BigDecimal averageRatingGiven;
    private Long totalReviewsGiven;
    
    // Rating distribution (how user rates chargers)
    private RatingDistribution ratingDistribution;
    
    // Recent ratings
    private List<RecentRatingDTO> recentRatings;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentRatingDTO {
        private Long ratingId;
        private Long chargerId;
        private String chargerName;
        private Integer rating;
        private String comment;
        private LocalDateTime date;
    }
}
