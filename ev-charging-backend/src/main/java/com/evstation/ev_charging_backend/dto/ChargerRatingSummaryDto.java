package com.evstation.ev_charging_backend.dto;

import lombok.*;

/**
 * Data Transfer Object for charger rating summary.
 * Contains aggregated rating statistics for a charger.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargerRatingSummaryDto {

    /**
     * Charger ID
     */
    private Long chargerId;

    /**
     * Charger name
     */
    private String chargerName;

    /**
     * Average rating score (rounded to 2 decimal places)
     */
    private Double averageRating;

    /**
     * Total number of ratings received
     */
    private Long totalRatings;

    /**
     * Number of 5-star ratings
     */
    private Long fiveStarCount;

    /**
     * Number of 4-star ratings
     */
    private Long fourStarCount;

    /**
     * Number of 3-star ratings
     */
    private Long threeStarCount;

    /**
     * Number of 2-star ratings
     */
    private Long twoStarCount;

    /**
     * Number of 1-star ratings
     */
    private Long oneStarCount;
}