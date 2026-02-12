package com.evstation.ev_charging_backend.repository;

import com.evstation.ev_charging_backend.entity.Charger;
import com.evstation.ev_charging_backend.entity.Payment;
import com.evstation.ev_charging_backend.entity.Rating;
import com.evstation.ev_charging_backend.enums.PaymentStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChargerRepository extends JpaRepository<Charger, Long> {

    List<Charger> findByHostUserId(Long hostId);

    // 🔍 Search query with optional filters
    @Query("""
        SELECT c FROM Charger c
        WHERE (:brand IS NULL OR LOWER(c.brand) LIKE LOWER(CONCAT('%', :brand, '%')))
        AND (:location IS NULL OR LOWER(c.location) LIKE LOWER(CONCAT('%', :location, '%')))
        AND (:minPrice IS NULL OR c.pricePerKwh >= :minPrice)
        AND (:maxPrice IS NULL OR c.pricePerKwh <= :maxPrice)
        """)
    List<Charger> searchChargers(
            @Param("brand") String brand,
            @Param("location") String location,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice
    );

    // 🔒 Pessimistic lock for booking operations - RETURNS Charger directly
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Charger c WHERE c.id = :id")
    Charger findByIdForUpdate(@Param("id") Long id);

    // ============================================
    // HOST ANALYTICS QUERIES
    // ============================================

    /**
     * Get total charger count for a host
     */
    @Query("""
        SELECT COUNT(c)
        FROM Charger c
        WHERE c.host.userId = :hostId
    """)
    Long getTotalChargersByHost(@Param("hostId") Long hostId);

    /**
     * Get all chargers for a host with basic info and ratings
     * 
     * ⭐ FIXED: Uses COALESCE to handle new chargers with no ratings
     * - Returns 0.0 instead of NULL when AVG(r.ratingScore) has no data
     * - Prevents NullPointerException when processing new chargers
     * - Ensures analytics endpoint works even with brand new chargers
     */
    @Query("""
        SELECT c.id, c.name, c.brand, c.location, COALESCE(AVG(r.ratingScore), 0.0)
        FROM Charger c
        LEFT JOIN Rating r ON r.charger.id = c.id
        WHERE c.host.userId = :hostId
        GROUP BY c.id, c.name, c.brand, c.location
    """)
    List<Object[]> getAllChargersByHost(@Param("hostId") Long hostId);

    /**
     * Get chargers with last booking date
     */
    @Query("""
        SELECT c.id, MAX(b.endTime)
        FROM Charger c
        LEFT JOIN Booking b ON b.charger.id = c.id
        WHERE c.host.userId = :hostId
        GROUP BY c.id
    """)
    List<Object[]> getChargersWithLastBookingDate(@Param("hostId") Long hostId);

    /**
     * Get charger performance metrics (for analytics)
     */
    @Query("""
        SELECT c.id, c.name,
               COUNT(b) as totalBookings,
               COALESCE(SUM(CASE WHEN b.status = 'COMPLETED' THEN b.totalPrice ELSE 0 END), 0) as revenue
        FROM Charger c
        LEFT JOIN Booking b ON b.charger.id = c.id
            AND b.createdAt >= :startDate
        WHERE c.host.userId = :hostId
        GROUP BY c.id, c.name
        ORDER BY revenue DESC
    """)
    List<Object[]> getChargerPerformance(
        @Param("hostId") Long hostId,
        @Param("startDate") LocalDateTime startDate
    );


    // Add these methods to ChargerRepository interface:
    
    /**
     * Count available chargers (chargers without active bookings)
     * A charger is considered available if it has no ongoing/active bookings
     * Excludes chargers with: RESERVED, PAYMENT_PENDING, CONFIRMED, or ACTIVE bookings
     */
    @Query("""
        SELECT COUNT(c)
        FROM Charger c
        WHERE c.id NOT IN (
            SELECT b.charger.id
            FROM Booking b
            WHERE b.status IN ('RESERVED', 'PAYMENT_PENDING', 'CONFIRMED', 'ACTIVE')
        )
        """)
    Long countAvailableChargers();

    @Query("SELECT AVG(c.rating) FROM Charger c WHERE c.rating IS NOT NULL")
    Optional<Double> findAverageRating();

    /**
     * ❌ COMMENTED OUT - Field 'chargerType' doesn't exist in Charger entity
     * 
     * To fix this method:
     * 1. Check your Charger entity for the correct field name (e.g., 'type', 'connectorType', etc.)
     * 2. Replace 'chargerType' with the actual field name
     * 3. Uncomment the method
     * 
     * Example fix if the field is called 'type':
     * @Query("""
     *     SELECT c.type, COUNT(c)
     *     FROM Charger c
     *     GROUP BY c.type
     *     ORDER BY COUNT(c) DESC
     *     """)
     * List<Object[]> getChargerTypeDistribution();
     */
    // @Query("""
    //     SELECT c.chargerType, COUNT(c)
    //     FROM Charger c
    //     GROUP BY c.chargerType
    //     ORDER BY COUNT(c) DESC
    //     """)
    // List<Object[]> getChargerTypeDistribution();

    /**
     * Get top locations by bookings
     * Returns: [location, chargerCount, bookingCount, revenue]
     */
    @Query("""
        SELECT c.location,
               COUNT(DISTINCT c.id) as chargerCount,
               COUNT(b) as bookingCount,
               COALESCE(SUM(b.totalPrice), 0) as revenue
        FROM Charger c
        LEFT JOIN Booking b ON b.charger.id = c.id
            AND b.status = 'COMPLETED'
            AND b.createdAt BETWEEN :startDate AND :endDate
        GROUP BY c.location
        ORDER BY COUNT(b) DESC
        """)
    List<Object[]> findTopLocationsByBookings(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
}