package com.evstation.ev_charging_backend.repository;

import com.evstation.ev_charging_backend.entity.Charger;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

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
}