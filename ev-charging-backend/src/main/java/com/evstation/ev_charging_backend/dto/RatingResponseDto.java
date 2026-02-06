package com.evstation.ev_charging_backend.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for returning rating information.
 * Contains complete rating details including user and charger information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingResponseDto {

    /**
     * Unique identifier of the rating
     */
    private Long id;

    /**
     * ID of the user who submitted the rating
     */
    private Long userId;

    /**
     * Full name of the user who submitted the rating
     */
    private String userName;

    /**
     * ID of the charger being rated
     */
    private Long chargerId;

    /**
     * Name of the charger being rated
     */
    private String chargerName;

    /**
     * ID of the booking this rating is associated with
     */
    private Long bookingId;

    /**
     * Rating score (1-5 stars)
     */
    private Integer ratingScore;

    /**
     * Optional review comment
     */
    private String comment;

    /**
     * Timestamp when the rating was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the rating was last updated
     */
    private LocalDateTime updatedAt;
}