package com.evstation.ev_charging_backend.service;

import com.evstation.ev_charging_backend.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for rating operations.
 * Defines business logic methods for managing ratings.
 */
public interface RatingService {

    /**
     * Create a new rating for a completed booking
     * 
     * @param requestDto Rating details including booking ID, score, and comment
     * @param userEmail Email of the authenticated user
     * @return Created rating details
     * @throws InvalidRatingException if booking is not completed or doesn't belong to user
     * @throws RatingAlreadyExistsException if rating already exists for this booking
     */
    RatingResponseDto createRating(RatingRequestDto requestDto, String userEmail);

    /**
     * Update an existing rating
     * 
     * @param ratingId ID of the rating to update
     * @param requestDto Updated rating details
     * @param userEmail Email of the authenticated user
     * @return Updated rating details
     * @throws RatingNotFoundException if rating doesn't exist
     * @throws UnauthorizedRatingAccessException if user doesn't own this rating
     */
    RatingResponseDto updateRating(Long ratingId, RatingRequestDto requestDto, String userEmail);

    /**
     * Delete a rating
     * 
     * @param ratingId ID of the rating to delete
     * @param userEmail Email of the authenticated user
     * @throws RatingNotFoundException if rating doesn't exist
     * @throws UnauthorizedRatingAccessException if user doesn't own this rating
     */
    void deleteRating(Long ratingId, String userEmail);

    /**
     * Get rating by ID
     * 
     * @param ratingId ID of the rating
     * @return Rating details
     * @throws RatingNotFoundException if rating doesn't exist
     */
    RatingResponseDto getRatingById(Long ratingId);

    /**
     * Get rating for a specific booking
     * 
     * @param bookingId ID of the booking
     * @return Rating details if exists
     * @throws RatingNotFoundException if no rating exists for this booking
     */
    RatingResponseDto getRatingByBookingId(Long bookingId);

    /**
     * Get all ratings for a charger with pagination
     * 
     * @param chargerId ID of the charger
     * @param pageable Pagination parameters
     * @return Page of ratings
     */
    Page<RatingResponseDto> getRatingsByChargerId(Long chargerId, Pageable pageable);

    /**
     * Get all ratings by the authenticated user with pagination
     * 
     * @param userEmail Email of the authenticated user
     * @param pageable Pagination parameters
     * @return Page of user's ratings
     */
    Page<RatingResponseDto> getMyRatings(String userEmail, Pageable pageable);

    /**
     * Get rating summary and statistics for a charger
     * 
     * @param chargerId ID of the charger
     * @return Summary including average rating, total count, and star distribution
     */
    ChargerRatingSummaryDto getChargerRatingSummary(Long chargerId);

    /**
     * Check if a user can rate a specific booking
     * 
     * @param bookingId ID of the booking
     * @param userEmail Email of the authenticated user
     * @return true if user can rate this booking, false otherwise
     */
    boolean canUserRateBooking(Long bookingId, String userEmail);
}