package com.evstation.ev_charging_backend.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Data Transfer Object for creating or updating a rating.
 * Used when a user submits a new rating or modifies an existing one.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingRequestDto {

    /**
     * The booking ID that this rating is for
     * Required when creating a new rating
     */
    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    /**
     * Rating score from 1 to 5 stars
     */
    @NotNull(message = "Rating score is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must not exceed 5")
    private Integer ratingScore;

    /**
     * Optional review comment
     * Maximum 1000 characters
     */
    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;
}