package com.evstation.ev_charging_backend.serviceImpl;

import com.evstation.ev_charging_backend.dto.*;
import com.evstation.ev_charging_backend.entity.*;
import com.evstation.ev_charging_backend.enums.BookingStatus;
import com.evstation.ev_charging_backend.exception.*;
import com.evstation.ev_charging_backend.repository.*;
import com.evstation.ev_charging_backend.service.RatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of RatingService.
 * Handles all rating-related business logic including validation,
 * rating calculations, and charger rating updates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ChargerRepository chargerRepository;

    @Override
    @Transactional
    public RatingResponseDto createRating(RatingRequestDto requestDto, String userEmail) {
        log.info("Creating rating for booking ID: {} by user: {}", requestDto.getBookingId(), userEmail);

        // 1. Get authenticated user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. Get and validate booking
        Booking booking = bookingRepository.findById(requestDto.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + requestDto.getBookingId()));

        // 3. Validate booking belongs to user
        if (!booking.getUser().getUserId().equals(user.getUserId())) {
            throw new InvalidRatingException("You can only rate your own bookings");
        }

        // 4. Validate booking is completed
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new InvalidRatingException("You can only rate completed bookings. Current status: " + booking.getStatus());
        }

        // 5. Check if rating already exists for this booking
        if (ratingRepository.existsByBookingId(booking.getId())) {
            throw new RatingAlreadyExistsException(booking.getId());
        }

        // 6. Get charger from booking
        Charger charger = booking.getCharger();

        // 7. Create and save rating
        Rating rating = Rating.builder()
                .user(user)
                .charger(charger)
                .booking(booking)
                .ratingScore(requestDto.getRatingScore())
                .comment(requestDto.getComment())
                .build();

        Rating savedRating = ratingRepository.save(rating);
        log.info("Rating created successfully with ID: {}", savedRating.getId());

        // 8. Update charger's average rating
        updateChargerAverageRating(charger.getId());

        // 9. Return response DTO
        return mapToResponseDto(savedRating);
    }

    @Override
    @Transactional
    public RatingResponseDto updateRating(Long ratingId, RatingRequestDto requestDto, String userEmail) {
        log.info("Updating rating ID: {} by user: {}", ratingId, userEmail);

        // 1. Get authenticated user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. Get existing rating
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RatingNotFoundException(ratingId));

        // 3. Validate user owns this rating
        if (!rating.getUser().getUserId().equals(user.getUserId())) {
            throw new UnauthorizedRatingAccessException();
        }

        // 4. Update rating fields
        rating.setRatingScore(requestDto.getRatingScore());
        rating.setComment(requestDto.getComment());

        Rating updatedRating = ratingRepository.save(rating);
        log.info("Rating updated successfully: {}", ratingId);

        // 5. Recalculate charger's average rating
        updateChargerAverageRating(rating.getCharger().getId());

        // 6. Return response DTO
        return mapToResponseDto(updatedRating);
    }

    @Override
    @Transactional
    public void deleteRating(Long ratingId, String userEmail) {
        log.info("Deleting rating ID: {} by user: {}", ratingId, userEmail);

        // 1. Get authenticated user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. Get existing rating
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RatingNotFoundException(ratingId));

        // 3. Validate user owns this rating
        if (!rating.getUser().getUserId().equals(user.getUserId())) {
            throw new UnauthorizedRatingAccessException();
        }

        Long chargerId = rating.getCharger().getId();

        // 4. Delete rating
        ratingRepository.delete(rating);
        log.info("Rating deleted successfully: {}", ratingId);

        // 5. Recalculate charger's average rating
        updateChargerAverageRating(chargerId);
    }

    @Override
    @Transactional(readOnly = true)
    public RatingResponseDto getRatingById(Long ratingId) {
        log.debug("Fetching rating by ID: {}", ratingId);

        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RatingNotFoundException(ratingId));

        return mapToResponseDto(rating);
    }

    @Override
    @Transactional(readOnly = true)
    public RatingResponseDto getRatingByBookingId(Long bookingId) {
        log.debug("Fetching rating for booking ID: {}", bookingId);

        Rating rating = ratingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RatingNotFoundException("No rating found for booking ID: " + bookingId));

        return mapToResponseDto(rating);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RatingResponseDto> getRatingsByChargerId(Long chargerId, Pageable pageable) {
        log.debug("Fetching ratings for charger ID: {} with page: {}", chargerId, pageable.getPageNumber());

        // Verify charger exists
        if (!chargerRepository.existsById(chargerId)) {
            throw new ResourceNotFoundException("Charger not found with ID: " + chargerId);
        }

        Page<Rating> ratings = ratingRepository.findByChargerIdWithUser(chargerId, pageable);
        return ratings.map(this::mapToResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RatingResponseDto> getMyRatings(String userEmail, Pageable pageable) {
        log.debug("Fetching ratings for user: {} with page: {}", userEmail, pageable.getPageNumber());

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Page<Rating> ratings = ratingRepository.findByUserIdWithCharger(user.getUserId(), pageable);
        return ratings.map(this::mapToResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ChargerRatingSummaryDto getChargerRatingSummary(Long chargerId) {
        log.debug("Fetching rating summary for charger ID: {}", chargerId);

        // Verify charger exists and get details
        Charger charger = chargerRepository.findById(chargerId)
                .orElseThrow(() -> new ResourceNotFoundException("Charger not found with ID: " + chargerId));

        // Get rating distribution
        List<Object[]> distribution = ratingRepository.getRatingDistribution(chargerId);
        
        // Create a map for easy access
        Map<Integer, Long> countByScore = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            countByScore.put(i, 0L);
        }
        
        long totalRatings = 0;
        for (Object[] row : distribution) {
            Integer score = (Integer) row[0];
            Long count = (Long) row[1];
            countByScore.put(score, count);
            totalRatings += count;
        }

        // Get average rating from database (more efficient than calculating from all records)
        Double averageRating = ratingRepository.calculateAverageRating(chargerId);
        if (averageRating == null) {
            averageRating = 0.0;
        }

        // Round to 2 decimal places
        BigDecimal roundedAverage = BigDecimal.valueOf(averageRating)
                .setScale(2, RoundingMode.HALF_UP);

        return ChargerRatingSummaryDto.builder()
                .chargerId(chargerId)
                .chargerName(charger.getName())
                .averageRating(roundedAverage.doubleValue())
                .totalRatings(totalRatings)
                .fiveStarCount(countByScore.get(5))
                .fourStarCount(countByScore.get(4))
                .threeStarCount(countByScore.get(3))
                .twoStarCount(countByScore.get(2))
                .oneStarCount(countByScore.get(1))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserRateBooking(Long bookingId, String userEmail) {
        log.debug("Checking if user {} can rate booking {}", userEmail, bookingId);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + bookingId));

        // Check if booking belongs to user
        if (!booking.getUser().getUserId().equals(user.getUserId())) {
            return false;
        }

        // Check if booking is completed
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            return false;
        }

        // Check if rating already exists
        return !ratingRepository.existsByBookingId(bookingId);
    }

    /**
     * Update the average rating for a charger
     * Called after creating, updating, or deleting a rating
     */
    @Transactional
    protected void updateChargerAverageRating(Long chargerId) {
        log.debug("Updating average rating for charger ID: {}", chargerId);

        Charger charger = chargerRepository.findById(chargerId)
                .orElseThrow(() -> new ResourceNotFoundException("Charger not found with ID: " + chargerId));

        Double averageRating = ratingRepository.calculateAverageRating(chargerId);
        
        // If no ratings exist, set to 0.0
        if (averageRating == null) {
            averageRating = 0.0;
        }

        // Round to 2 decimal places for consistency
        BigDecimal roundedAverage = BigDecimal.valueOf(averageRating)
                .setScale(2, RoundingMode.HALF_UP);

        charger.setRating(roundedAverage.doubleValue());
        chargerRepository.save(charger);

        log.info("Updated charger {} average rating to: {}", chargerId, roundedAverage);
    }

    /**
     * Map Rating entity to RatingResponseDto
     */
    private RatingResponseDto mapToResponseDto(Rating rating) {
        User user = rating.getUser();
        Charger charger = rating.getCharger();
        
        String userName = (user.getFirstName() != null && user.getLastName() != null)
                ? user.getFirstName() + " " + user.getLastName()
                : user.getEmail();

        return RatingResponseDto.builder()
                .id(rating.getId())
                .userId(user.getUserId())
                .userName(userName)
                .chargerId(charger.getId())
                .chargerName(charger.getName())
                .bookingId(rating.getBooking().getId())
                .ratingScore(rating.getRatingScore())
                .comment(rating.getComment())
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .build();
    }
}