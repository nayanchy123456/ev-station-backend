package com.evstation.ev_charging_backend.controller;

import com.evstation.ev_charging_backend.dto.*;
import com.evstation.ev_charging_backend.security.JwtUtil;
import com.evstation.ev_charging_backend.service.RatingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for rating operations.
 * Provides endpoints for creating, updating, deleting, and retrieving ratings.
 */
@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
@Slf4j
public class RatingController {

    private final RatingService ratingService;
    private final JwtUtil jwtUtil;

    /**
     * Create a new rating for a completed booking
     * POST /api/ratings
     * 
     * @param requestDto Rating details (bookingId, ratingScore, comment)
     * @param request HTTP request containing JWT token
     * @return Created rating with 201 CREATED status
     */
    @PostMapping
    public ResponseEntity<RatingResponseDto> createRating(
            @Valid @RequestBody RatingRequestDto requestDto,
            HttpServletRequest request) {
        
        log.info("POST /api/ratings - Creating rating for booking: {}", requestDto.getBookingId());
        
        String userEmail = extractUserEmail(request);
        RatingResponseDto response = ratingService.createRating(requestDto, userEmail);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing rating
     * PUT /api/ratings/{id}
     * 
     * @param id Rating ID to update
     * @param requestDto Updated rating details
     * @param request HTTP request containing JWT token
     * @return Updated rating details
     */
    @PutMapping("/{id}")
    public ResponseEntity<RatingResponseDto> updateRating(
            @PathVariable Long id,
            @Valid @RequestBody RatingRequestDto requestDto,
            HttpServletRequest request) {
        
        log.info("PUT /api/ratings/{} - Updating rating", id);
        
        String userEmail = extractUserEmail(request);
        RatingResponseDto response = ratingService.updateRating(id, requestDto, userEmail);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a rating
     * DELETE /api/ratings/{id}
     * 
     * @param id Rating ID to delete
     * @param request HTTP request containing JWT token
     * @return Success message with 200 OK status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteRating(
            @PathVariable Long id,
            HttpServletRequest request) {
        
        log.info("DELETE /api/ratings/{} - Deleting rating", id);
        
        String userEmail = extractUserEmail(request);
        ratingService.deleteRating(id, userEmail);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Rating deleted successfully");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get rating by ID
     * GET /api/ratings/{id}
     * 
     * @param id Rating ID
     * @return Rating details
     */
    @GetMapping("/{id}")
    public ResponseEntity<RatingResponseDto> getRatingById(@PathVariable Long id) {
        log.debug("GET /api/ratings/{} - Fetching rating", id);
        
        RatingResponseDto response = ratingService.getRatingById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get rating for a specific booking
     * GET /api/ratings/booking/{bookingId}
     * 
     * @param bookingId Booking ID
     * @return Rating details if exists
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<RatingResponseDto> getRatingByBookingId(@PathVariable Long bookingId) {
        log.debug("GET /api/ratings/booking/{} - Fetching rating for booking", bookingId);
        
        RatingResponseDto response = ratingService.getRatingByBookingId(bookingId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all ratings for a specific charger with pagination
     * GET /api/ratings/charger/{chargerId}?page=0&size=10
     * 
     * @param chargerId Charger ID
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @return Page of ratings for the charger
     */
    @GetMapping("/charger/{chargerId}")
    public ResponseEntity<Page<RatingResponseDto>> getRatingsByChargerId(
            @PathVariable Long chargerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.debug("GET /api/ratings/charger/{} - Fetching ratings (page: {}, size: {})", chargerId, page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<RatingResponseDto> ratings = ratingService.getRatingsByChargerId(chargerId, pageable);
        
        return ResponseEntity.ok(ratings);
    }

    /**
     * Get all ratings by the authenticated user with pagination
     * GET /api/ratings/my-ratings?page=0&size=10
     * 
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @param request HTTP request containing JWT token
     * @return Page of user's ratings
     */
    @GetMapping("/my-ratings")
    public ResponseEntity<Page<RatingResponseDto>> getMyRatings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        
        log.debug("GET /api/ratings/my-ratings - Fetching user's ratings (page: {}, size: {})", page, size);
        
        String userEmail = extractUserEmail(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<RatingResponseDto> ratings = ratingService.getMyRatings(userEmail, pageable);
        
        return ResponseEntity.ok(ratings);
    }

    /**
     * Get rating summary and statistics for a charger
     * GET /api/ratings/charger/{chargerId}/summary
     * 
     * @param chargerId Charger ID
     * @return Summary including average rating, total count, and star distribution
     */
    @GetMapping("/charger/{chargerId}/summary")
    public ResponseEntity<ChargerRatingSummaryDto> getChargerRatingSummary(@PathVariable Long chargerId) {
        log.debug("GET /api/ratings/charger/{}/summary - Fetching rating summary", chargerId);
        
        ChargerRatingSummaryDto summary = ratingService.getChargerRatingSummary(chargerId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Check if a user can rate a specific booking
     * GET /api/ratings/can-rate/{bookingId}
     * 
     * @param bookingId Booking ID
     * @param request HTTP request containing JWT token
     * @return Boolean indicating if user can rate the booking
     */
    @GetMapping("/can-rate/{bookingId}")
    public ResponseEntity<Map<String, Boolean>> canRateBooking(
            @PathVariable Long bookingId,
            HttpServletRequest request) {
        
        log.debug("GET /api/ratings/can-rate/{} - Checking if user can rate", bookingId);
        
        String userEmail = extractUserEmail(request);
        boolean canRate = ratingService.canUserRateBooking(bookingId, userEmail);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("canRate", canRate);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Extract user email from JWT token in the request
     * 
     * @param request HTTP request
     * @return User email from token
     * @throws SecurityException if token is invalid or missing
     */
    private String extractUserEmail(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Missing or invalid Authorization header");
        }
        
        String token = authHeader.substring(7);
        String email = jwtUtil.extractUsername(token);
        
        if (!jwtUtil.validateToken(token, email)) {
            throw new SecurityException("Invalid or expired token");
        }
        
        if (email == null || email.isEmpty()) {
            throw new SecurityException("Email not found in token");
        }
        
        return email;
    }
}