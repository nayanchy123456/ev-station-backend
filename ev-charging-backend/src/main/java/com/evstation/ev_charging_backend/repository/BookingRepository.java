package com.evstation.ev_charging_backend.repository;

import com.evstation.ev_charging_backend.entity.Booking;
import com.evstation.ev_charging_backend.enums.BookingStatus;
import jakarta.persistence.LockModeType;

import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Check overlapping bookings - UPDATED to include RESERVED and PAYMENT_PENDING
    @Query("""
                SELECT b FROM Booking b
                WHERE b.charger.id = :chargerId
                AND b.status IN :activeStatuses
                AND (:startTime < b.endTime AND :endTime > b.startTime)
            """)
    List<Booking> findConflictingBookings(
            @Param("chargerId") Long chargerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("activeStatuses") List<BookingStatus> activeStatuses);

    List<Booking> findByUserUserIdOrderByStartTimeDesc(Long userId);

    List<Booking> findByChargerIdOrderByStartTimeAsc(Long chargerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    List<Booking> findByStatusAndStartTimeBefore(BookingStatus status, LocalDateTime time);

    List<Booking> findByStatusAndEndTimeBefore(BookingStatus status, LocalDateTime time);

    List<Booking> findByChargerHostUserIdOrderByStartTimeDesc(Long hostId);

    List<Booking> findByStatusIn(List<BookingStatus> statuses);

    // NEW: Find expired reservations
    List<Booking> findByStatusAndReservedUntilBefore(BookingStatus status, LocalDateTime time);

    /*
     * ADD THESE METHODS TO: BookingRepository.java
     * Location:
     * src/main/java/com/evstation/ev_charging_backend/repository/BookingRepository.
     * java
     */

    // ============================================
    // HOST ANALYTICS QUERIES
    // ============================================

    /**
     * Get total revenue for a host in a date range
     */
    @Query("""
                SELECT COALESCE(SUM(b.totalPrice), 0)
                FROM Booking b
                WHERE b.charger.host.userId = :hostId
                AND b.status = 'COMPLETED'
                AND b.createdAt >= :startDate
            """)
    BigDecimal getTotalRevenueByHost(
            @Param("hostId") Long hostId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Get unique active users for a host in a date range
     */
    @Query("""
                SELECT COUNT(DISTINCT b.user.userId)
                FROM Booking b
                WHERE b.charger.host.userId = :hostId
                AND b.createdAt >= :startDate
            """)
    Long getActiveUserCountByHost(
            @Param("hostId") Long hostId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Get total bookings count for a host in a date range
     */
    @Query("""
                SELECT COUNT(b)
                FROM Booking b
                WHERE b.charger.host.userId = :hostId
                AND b.createdAt >= :startDate
            """)
    Long getTotalBookingsByHost(
            @Param("hostId") Long hostId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Get completed bookings count for a host
     */
    @Query("""
                SELECT COUNT(b)
                FROM Booking b
                WHERE b.charger.host.userId = :hostId
                AND b.status = 'COMPLETED'
                AND b.createdAt >= :startDate
            """)
    Long getCompletedBookingsByHost(
            @Param("hostId") Long hostId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Get daily revenue data for charts
     */
    @Query("""
                SELECT DATE(b.createdAt) as date,
                       SUM(b.totalPrice) as revenue,
                       COUNT(b) as bookingCount
                FROM Booking b
                WHERE b.charger.host.userId = :hostId
                AND b.status = 'COMPLETED'
                AND b.createdAt >= :startDate
                GROUP BY DATE(b.createdAt)
                ORDER BY DATE(b.createdAt)
            """)
    List<Object[]> getDailyRevenueByHost(
            @Param("hostId") Long hostId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Get revenue by charger for a host
     */
    @Query("""
                SELECT c.id,
                       c.name,
                       COALESCE(SUM(b.totalPrice), 0),
                       COUNT(b)
                FROM Charger c
                LEFT JOIN Booking b ON b.charger.id = c.id
                    AND b.status = 'COMPLETED'
                    AND b.createdAt >= :startDate
                WHERE c.host.userId = :hostId
                GROUP BY c.id, c.name
                ORDER BY SUM(b.totalPrice) DESC
            """)
    List<Object[]> getRevenueByCharger(
            @Param("hostId") Long hostId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Get booking counts by charger
     */
    @Query("""
                SELECT c.id, COUNT(b)
                FROM Charger c
                LEFT JOIN Booking b ON b.charger.id = c.id
                    AND b.createdAt >= :startDate
                WHERE c.host.userId = :hostId
                GROUP BY c.id
                ORDER BY COUNT(b) DESC
            """)
    List<Object[]> getBookingCountsByCharger(
            @Param("hostId") Long hostId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Get booking status distribution for host
     */
    @Query("""
                SELECT b.status, COUNT(b)
                FROM Booking b
                WHERE b.charger.host.userId = :hostId
                AND b.createdAt >= :startDate
                GROUP BY b.status
            """)
    List<Object[]> getBookingStatusDistribution(
            @Param("hostId") Long hostId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Get expired bookings with potential revenue
     */
    @Query("""
                SELECT COUNT(b), COALESCE(SUM(b.totalPrice), 0)
                FROM Booking b
                WHERE b.charger.host.userId = :hostId
                AND b.status = 'EXPIRED'
                AND b.createdAt >= :startDate
            """)
    Object[] getExpiredBookingsAnalysis(
            @Param("hostId") Long hostId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Get most frequent users per charger
     */
    @Query("""
                SELECT u.userId, u.firstName, u.lastName, u.email,
                       c.id, COUNT(b), SUM(b.totalPrice)
                FROM Booking b
                JOIN b.user u
                JOIN b.charger c
                WHERE c.host.userId = :hostId
                AND b.createdAt >= :startDate
                GROUP BY u.userId, u.firstName, u.lastName, u.email, c.id
                ORDER BY c.id, COUNT(b) DESC
            """)
    List<Object[]> getMostFrequentUsersByCharger(
            @Param("hostId") Long hostId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Get top users across all chargers
     */
    @Query("""
                SELECT u.userId, u.firstName, u.lastName, u.email,
                       COUNT(b), SUM(b.totalPrice)
                FROM Booking b
                JOIN b.user u
                WHERE b.charger.host.userId = :hostId
                AND b.createdAt >= :startDate
                GROUP BY u.userId, u.firstName, u.lastName, u.email
                ORDER BY COUNT(b) DESC
            """)
    List<Object[]> getTopUsersByHost(
            @Param("hostId") Long hostId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Get user's favorite charger (most visited)
     */
    @Query("""
                SELECT c.id, c.name, COUNT(b)
                FROM Booking b
                JOIN b.charger c
                WHERE b.user.userId = :userId
                AND c.host.userId = :hostId
                AND b.createdAt >= :startDate
                GROUP BY c.id, c.name
                ORDER BY COUNT(b) DESC
            """)
    List<Object[]> getUserFavoriteChargerForHost(
            @Param("userId") Long userId,
            @Param("hostId") Long hostId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Get user-charger affinity matrix
     */
    @Query("""
                SELECT u.userId, CONCAT(u.firstName, ' ', u.lastName),
                       c.id, c.name,
                       COUNT(b), SUM(b.totalPrice)
                FROM Booking b
                JOIN b.user u
                JOIN b.charger c
                WHERE c.host.userId = :hostId
                AND b.createdAt >= :startDate
                GROUP BY u.userId, u.firstName, u.lastName, c.id, c.name
                HAVING COUNT(b) > 0
                ORDER BY COUNT(b) DESC
            """)
    List<Object[]> getUserChargerAffinity(
            @Param("hostId") Long hostId,
            @Param("startDate") LocalDateTime startDate);

    // ============================================
    // USER ANALYTICS QUERIES
    // ============================================

    /**
     * Get total spending for a user
     */
    @Query("""
                SELECT COALESCE(SUM(b.totalPrice), 0)
                FROM Booking b
                WHERE b.user.userId = :userId
                AND b.status = 'COMPLETED'
                AND b.createdAt >= :startDate
            """)
    BigDecimal getTotalSpendingByUser(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Get total bookings for a user
     */
    @Query("""
                SELECT COUNT(b)
                FROM Booking b
                WHERE b.user.userId = :userId
                AND b.createdAt >= :startDate
            """)
    Long getTotalBookingsByUser(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Get daily spending for user
     */
    @Query("""
                SELECT DATE(b.createdAt) as date,
                       SUM(b.totalPrice) as spending,
                       COUNT(b) as bookingCount
                FROM Booking b
                WHERE b.user.userId = :userId
                AND b.status = 'COMPLETED'
                AND b.createdAt >= :startDate
                GROUP BY DATE(b.createdAt)
                ORDER BY DATE(b.createdAt)
            """)
    List<Object[]> getDailySpendingByUser(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Get spending by charger for user
     */
    @Query("""
                SELECT c.id, c.name,
                       SUM(b.totalPrice),
                       COUNT(b)
                FROM Booking b
                JOIN b.charger c
                WHERE b.user.userId = :userId
                AND b.status = 'COMPLETED'
                AND b.createdAt >= :startDate
                GROUP BY c.id, c.name
                ORDER BY SUM(b.totalPrice) DESC
            """)
    List<Object[]> getSpendingByCharger(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Get user's most visited chargers
     */
    @Query("""
                SELECT c.id, c.name, c.brand, c.location,
                       COUNT(b),
                       SUM(b.totalPrice),
                       MAX(b.createdAt)
                FROM Booking b
                JOIN b.charger c
                WHERE b.user.userId = :userId
                AND b.createdAt >= :startDate
                GROUP BY c.id, c.name, c.brand, c.location
                ORDER BY COUNT(b) DESC
            """)
    List<Object[]> getMostVisitedChargersByUser(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Get booking status distribution for user
     */
    @Query("""
                SELECT b.status, COUNT(b)
                FROM Booking b
                WHERE b.user.userId = :userId
                AND b.createdAt >= :startDate
                GROUP BY b.status
            """)
    List<Object[]> getBookingStatusDistributionByUser(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Get recent bookings for user
     */
    @Query("""
                SELECT b.id, c.id, c.name,
                       b.startTime,
                       TIMESTAMPDIFF(HOUR, b.startTime, b.endTime),
                       b.totalPrice,
                       b.status,
                       b.totalEnergyKwh
                FROM Booking b
                JOIN b.charger c
                WHERE b.user.userId = :userId
                ORDER BY b.createdAt DESC
            """)
    List<Object[]> getRecentBookingsByUser(
            @Param("userId") Long userId,
            PageRequest pageable);

    /**
     * Get average session duration for user
     */
    @Query("""
                SELECT AVG(TIMESTAMPDIFF(HOUR, b.startTime, b.endTime))
                FROM Booking b
                WHERE b.user.userId = :userId
                AND b.status = 'COMPLETED'
                AND b.createdAt >= :startDate
            """)
    Double getAverageSessionDuration(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Get total energy consumed by user
     */
    @Query("""
                SELECT COALESCE(SUM(b.totalEnergyKwh), 0)
                FROM Booking b
                WHERE b.user.userId = :userId
                AND b.status = 'COMPLETED'
                AND b.createdAt >= :startDate
            """)
    Double getTotalEnergyConsumed(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate);


    /**
     * Get peak charging hour by user (MySQL version)
     * Returns hour of day (0-23) with most bookings
     */
    @Query(value = """
                SELECT HOUR(b.start_time) as hour, COUNT(*) as count
                FROM bookings b
                WHERE b.user_id = :userId
                AND b.status = 'COMPLETED'
                AND b.created_at >= :startDate
                GROUP BY HOUR(b.start_time)
                ORDER BY count DESC
                LIMIT 1
            """, nativeQuery = true)
    Object[] getPeakChargingHour(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Get peak charging day by user (MySQL version)
     * Returns day of week with most bookings
     */
    @Query(value = """
                SELECT 
                    CASE DAYOFWEEK(b.start_time)
                        WHEN 1 THEN 'Sunday'
                        WHEN 2 THEN 'Monday'
                        WHEN 3 THEN 'Tuesday'
                        WHEN 4 THEN 'Wednesday'
                        WHEN 5 THEN 'Thursday'
                        WHEN 6 THEN 'Friday'
                        WHEN 7 THEN 'Saturday'
                    END as day_name,
                    COUNT(*) as count
                FROM bookings b
                WHERE b.user_id = :userId
                AND b.status = 'COMPLETED'
                AND b.created_at >= :startDate
                GROUP BY DAYOFWEEK(b.start_time)
                ORDER BY count DESC
                LIMIT 1
            """, nativeQuery = true)
    Object[] getPeakChargingDay(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate);

}