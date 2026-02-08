package com.evstation.ev_charging_backend.repository;

import com.evstation.ev_charging_backend.entity.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Rating entity operations.
 * Provides optimized queries for rating management and analytics.
 */
@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    /**
     * Check if a rating already exists for a specific booking
     * Used to prevent duplicate ratings
     */
    boolean existsByBookingId(Long bookingId);

    /**
     * Find rating by booking ID
     */
    Optional<Rating> findByBookingId(Long bookingId);

    /**
     * Find all ratings for a specific charger with pagination
     * Uses JOIN FETCH to avoid N+1 query problem
     */
    @Query("SELECT r FROM Rating r " +
            "JOIN FETCH r.user " +
            "WHERE r.charger.id = :chargerId " +
            "ORDER BY r.createdAt DESC")
    Page<Rating> findByChargerIdWithUser(@Param("chargerId") Long chargerId, Pageable pageable);

    /**
     * Find all ratings by a specific user with pagination
     * Uses JOIN FETCH to eagerly load related entities
     */
    @Query("SELECT r FROM Rating r " +
            "JOIN FETCH r.charger " +
            "WHERE r.user.userId = :userId " +
            "ORDER BY r.createdAt DESC")
    Page<Rating> findByUserIdWithCharger(@Param("userId") Long userId, Pageable pageable);

    /**
     * Get all ratings for a charger (without pagination) for calculating statistics
     */
    List<Rating> findByChargerId(Long chargerId);

    /**
     * Calculate average rating for a charger
     * More efficient than fetching all ratings and calculating in code
     */
    @Query("SELECT AVG(r.ratingScore) FROM Rating r WHERE r.charger.id = :chargerId")
    Double calculateAverageRating(@Param("chargerId") Long chargerId);

    /**
     * Count total ratings for a charger
     */
    @Query("SELECT COUNT(r) FROM Rating r WHERE r.charger.id = :chargerId")
    Long countByChargerId(@Param("chargerId") Long chargerId);

    /**
     * Count ratings by score for a specific charger
     * Used for rating distribution (star breakdown)
     */
    @Query("SELECT COUNT(r) FROM Rating r WHERE r.charger.id = :chargerId AND r.ratingScore = :score")
    Long countByChargerIdAndRatingScore(@Param("chargerId") Long chargerId, @Param("score") Integer score);

    /**
     * Get rating statistics grouped by score for a charger
     * Returns results as Object[] with [ratingScore, count]
     */
    @Query("SELECT r.ratingScore, COUNT(r) FROM Rating r " +
            "WHERE r.charger.id = :chargerId " +
            "GROUP BY r.ratingScore " +
            "ORDER BY r.ratingScore DESC")
    List<Object[]> getRatingDistribution(@Param("chargerId") Long chargerId);

    /**
     * Find recent ratings for a charger (for display on charger details page)
     */
    @Query("SELECT r FROM Rating r " +
            "JOIN FETCH r.user " +
            "WHERE r.charger.id = :chargerId " +
            "ORDER BY r.createdAt DESC")
    List<Rating> findTop10ByChargerIdOrderByCreatedAtDesc(@Param("chargerId") Long chargerId, Pageable pageable);

    /**
     * Delete all ratings for a specific charger
     * Used when a charger is deleted (cascade)
     */
    void deleteByChargerId(Long chargerId);

    /**
     * Check if a user has already rated a specific charger
     * (through any of their bookings)
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Rating r " +
            "WHERE r.user.userId = :userId AND r.charger.id = :chargerId")
    boolean existsByUserIdAndChargerId(@Param("userId") Long userId, @Param("chargerId") Long chargerId);

    // ============================================
    // HOST ANALYTICS QUERIES
    // ============================================

    /**
     * Get average rating for a specific charger
     */
    @Query("""
        SELECT AVG(r.ratingScore)
        FROM Rating r
        WHERE r.charger.id = :chargerId
    """)
    Double getAverageRatingByCharger(@Param("chargerId") Long chargerId);

    /**
     * Get rating count for a specific charger
     */
    @Query("""
        SELECT COUNT(r)
        FROM Rating r
        WHERE r.charger.id = :chargerId
    """)
    Long getRatingCountByCharger(@Param("chargerId") Long chargerId);

    /**
     * Get rating distribution for a charger (1-5 stars)
     */
    @Query("""
        SELECT r.ratingScore, COUNT(r)
        FROM Rating r
        WHERE r.charger.id = :chargerId
        GROUP BY r.ratingScore
        ORDER BY r.ratingScore DESC
    """)
    List<Object[]> getRatingDistributionByCharger(@Param("chargerId") Long chargerId);

    /**
     * Get average rating across all chargers for a host
     */
    @Query("""
        SELECT AVG(r.ratingScore)
        FROM Rating r
        WHERE r.charger.host.userId = :hostId
    """)
    Double getAverageRatingByHost(@Param("hostId") Long hostId);

    // ============================================
    // USER ANALYTICS QUERIES
    // ============================================

    /**
     * Get average rating given by a user
     */
    @Query("""
        SELECT AVG(r.ratingScore)
        FROM Rating r
        WHERE r.user.userId = :userId
    """)
    Double getAverageRatingGivenByUser(@Param("userId") Long userId);

    /**
     * Get total reviews given by a user
     */
    @Query("""
        SELECT COUNT(r)
        FROM Rating r
        WHERE r.user.userId = :userId
    """)
    Long getTotalReviewsByUser(@Param("userId") Long userId);

    /**
     * Get rating distribution by user (how they rate)
     */
    @Query("""
        SELECT r.ratingScore, COUNT(r)
        FROM Rating r
        WHERE r.user.userId = :userId
        GROUP BY r.ratingScore
        ORDER BY r.ratingScore DESC
    """)
    List<Object[]> getRatingDistributionByUser(@Param("userId") Long userId);

    /**
     * Get recent ratings by user
     */
    @Query("""
        SELECT r.id, r.charger.id, r.charger.name,
               r.ratingScore, r.comment, r.createdAt
        FROM Rating r
        WHERE r.user.userId = :userId
        ORDER BY r.createdAt DESC
    """)
    List<Object[]> getRecentRatingsByUser(
            @Param("userId") Long userId,
            Pageable pageable);

    /**
     * Get average rating given by user to a specific charger
     */
    @Query("""
        SELECT AVG(r.ratingScore)
        FROM Rating r
        WHERE r.user.userId = :userId
        AND r.charger.id = :chargerId
    """)
    Double getAverageRatingGivenToCharger(
            @Param("userId") Long userId,
            @Param("chargerId") Long chargerId);
}