package com.evstation.ev_charging_backend.dto.analytics.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO for rating distribution (1-5 stars)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingDistribution {
    @Builder.Default
    private Map<Integer, RatingCount> distribution = new HashMap<>();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingCount {
        private Long count;
        private BigDecimal percentage;
    }
    
    /**
     * Add or update a rating count
     */
    public void setRating(Integer stars, Long count, BigDecimal percentage) {
        distribution.put(stars, RatingCount.builder()
                .count(count)
                .percentage(percentage)
                .build());
    }
}
