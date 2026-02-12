package com.evstation.ev_charging_backend.repository;

import com.evstation.ev_charging_backend.entity.Booking;
import com.evstation.ev_charging_backend.enums.BookingStatus;
import jakarta.persistence.LockModeType;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ============================================
    // BASIC QUERIES
    // ============================================

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

    Long countByStatus(BookingStatus status);
    
    Long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    Long countByStatusAndCreatedAtBetween(BookingStatus status, LocalDateTime start, LocalDateTime end);
    
    List<Booking> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    Long countByChargerId(Long chargerId);
    
    List<Booking> findByChargerIdAndStatus(Long chargerId, BookingStatus status);
    
    List<Booking> findByStatus(BookingStatus status);
    
    Optional<Booking> findTopByUserUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT b.user.userId, COUNT(b) FROM Booking b GROUP BY b.user.userId")
    List<Object[]> findUserBookingCounts();

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
    List<Object[]> getBookingStatusDistributionByHost(
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

    /**
     * Find top hosts by revenue
     */
    @Query("""
            SELECT h.userId, 
                   CONCAT(h.firstName, ' ', h.lastName) as hostName,
                   h.email,
                   COALESCE(SUM(b.totalPrice), 0) as totalRevenue,
                   COUNT(DISTINCT c.id) as chargerCount,
                   COUNT(b) as bookingCount
            FROM User h
            LEFT JOIN Charger c ON c.host.userId = h.userId
            LEFT JOIN Booking b ON b.charger.id = c.id 
                AND b.status = 'COMPLETED'
                AND b.createdAt BETWEEN :startDate AND :endDate
            WHERE h.role = 'HOST'
            GROUP BY h.userId, h.firstName, h.lastName, h.email
            ORDER BY SUM(b.totalPrice) DESC
            """)
    List<Object[]> findTopHostsByRevenue(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Find top rated hosts
     */
    @Query("""
            SELECT h.userId, 
                   CONCAT(h.firstName, ' ', h.lastName) as hostName,
                   h.email,
                   COALESCE(AVG(c.rating), 0.0) as averageRating,
                   COUNT(DISTINCT c.id) as chargerCount,
                   COUNT(b) as bookingCount
            FROM User h
            LEFT JOIN Charger c ON c.host.userId = h.userId
            LEFT JOIN Booking b ON b.charger.id = c.id 
                AND b.status = 'COMPLETED'
                AND b.createdAt BETWEEN :startDate AND :endDate
            WHERE h.role = 'HOST'
            GROUP BY h.userId, h.firstName, h.lastName, h.email
            HAVING AVG(c.rating) IS NOT NULL
            ORDER BY AVG(c.rating) DESC
            """)
    List<Object[]> findTopRatedHosts(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Find hosts with most bookings
     */
    @Query("""
            SELECT h.userId, 
                   CONCAT(h.firstName, ' ', h.lastName) as hostName,
                   h.email,
                   COUNT(b) as bookingCount,
                   COUNT(DISTINCT c.id) as chargerCount,
                   COALESCE(SUM(b.totalPrice), 0) as totalRevenue
            FROM User h
            LEFT JOIN Charger c ON c.host.userId = h.userId
            LEFT JOIN Booking b ON b.charger.id = c.id 
                AND b.createdAt BETWEEN :startDate AND :endDate
            WHERE h.role = 'HOST'
            GROUP BY h.userId, h.firstName, h.lastName, h.email
            ORDER BY COUNT(b) DESC
            """)
    List<Object[]> findHostsWithMostBookings(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Get host approval timeline
     */
    @Query(value = """
            SELECT DATE(u.created_at) as approval_date,
                   SUM(CASE WHEN u.role = 'HOST' THEN 1 ELSE 0 END) as approved,
                   SUM(CASE WHEN u.role = 'PENDING_HOST' THEN 1 ELSE 0 END) as pending,
                   0 as rejected
            FROM users u
            WHERE u.created_at BETWEEN :startDate AND :endDate
              AND u.role IN ('HOST', 'PENDING_HOST')
            GROUP BY DATE(u.created_at)
            ORDER BY DATE(u.created_at)
            """, nativeQuery = true)
    List<Object[]> getHostApprovalTimeline(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

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

    // ============================================
    // ADMIN/PLATFORM ANALYTICS QUERIES
    // ============================================

    /**
     * Get daily booking trend
     * Returns: [date, count]
     */
    @Query(value = """
            SELECT DATE(b.created_at) as date,
                   COUNT(*) as booking_count
            FROM bookings b
            WHERE b.created_at BETWEEN :startDate AND :endDate
            GROUP BY DATE(b.created_at)
            ORDER BY DATE(b.created_at)
            """, nativeQuery = true)
    List<Object[]> getDailyBookingTrend(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get top users by booking count
     * Returns: [User entity, bookingCount]
     */
    @Query("""
            SELECT b.user, COUNT(b)
            FROM Booking b
            GROUP BY b.user
            ORDER BY COUNT(b) DESC
            """)
    List<Object[]> findTopUsersByBookingCount(PageRequest pageable);

    default List<Object[]> findTopUsersByBookingCount(int limit) {
        return findTopUsersByBookingCount(PageRequest.of(0, limit));
    }

    /**
     * Get top users by booking count with last active in single query (OPTIMIZED)
     * Replaces N+1 query problem
     */
    @Query("""
            SELECT b.user.userId, b.user.firstName, b.user.lastName, b.user.email,
                   COUNT(b) as bookingCount,
                   MAX(b.createdAt) as lastActive
            FROM Booking b
            WHERE b.createdAt BETWEEN :startDate AND :endDate
            GROUP BY b.user.userId, b.user.firstName, b.user.lastName, b.user.email
            ORDER BY COUNT(b) DESC
            """)
    List<Object[]> findTopUsersByBookingCountOptimized(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Find top chargers by revenue
     * Returns: [chargerId, chargerName, brand, location, rating, pricePerKwh, hostName, revenue, bookingCount]
     */
    @Query("""
            SELECT c.id, c.name, c.brand, c.location, c.rating, c.pricePerKwh,
                   CONCAT(h.firstName, ' ', h.lastName) as hostName,
                   COALESCE(SUM(b.totalPrice), 0) as totalRevenue,
                   COUNT(b) as bookingCount
            FROM Charger c
            LEFT JOIN c.host h
            LEFT JOIN Booking b ON b.charger.id = c.id 
                AND b.status = 'COMPLETED'
                AND b.createdAt BETWEEN :startDate AND :endDate
            GROUP BY c.id, c.name, c.brand, c.location, c.rating, c.pricePerKwh, h.firstName, h.lastName
            ORDER BY SUM(b.totalPrice) DESC
            """)
    List<Object[]> findTopChargersByRevenue(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Find top chargers by booking count
     */
    @Query("""
            SELECT c.id, c.name, c.brand, c.location, c.rating, c.pricePerKwh,
                   CONCAT(h.firstName, ' ', h.lastName) as hostName,
                   COALESCE(SUM(b.totalPrice), 0) as totalRevenue,
                   COUNT(b) as bookingCount
            FROM Charger c
            LEFT JOIN c.host h
            LEFT JOIN Booking b ON b.charger.id = c.id 
                AND b.status = 'COMPLETED'
                AND b.createdAt BETWEEN :startDate AND :endDate
            GROUP BY c.id, c.name, c.brand, c.location, c.rating, c.pricePerKwh, h.firstName, h.lastName
            ORDER BY COUNT(b) DESC
            """)
    List<Object[]> findTopChargersByBookings(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Find top chargers by rating
     */
    @Query("""
            SELECT c.id, c.name, c.brand, c.location, c.rating, c.pricePerKwh,
                   CONCAT(h.firstName, ' ', h.lastName) as hostName,
                   COALESCE(SUM(b.totalPrice), 0) as totalRevenue,
                   COUNT(b) as bookingCount
            FROM Charger c
            LEFT JOIN c.host h
            LEFT JOIN Booking b ON b.charger.id = c.id 
                AND b.status = 'COMPLETED'
                AND b.createdAt BETWEEN :startDate AND :endDate
            GROUP BY c.id, c.name, c.brand, c.location, c.rating, c.pricePerKwh, h.firstName, h.lastName
            ORDER BY c.rating DESC
            """)
    List<Object[]> findTopChargersByRating(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Find underutilized chargers
     */
    @Query("""
            SELECT c.id, c.name, c.brand, c.location, c.rating, c.pricePerKwh,
                   CONCAT(h.firstName, ' ', h.lastName) as hostName,
                   COALESCE(SUM(b.totalPrice), 0) as totalRevenue,
                   COUNT(b) as bookingCount
            FROM Charger c
            LEFT JOIN c.host h
            LEFT JOIN Booking b ON b.charger.id = c.id 
                AND b.status = 'COMPLETED'
                AND b.createdAt BETWEEN :startDate AND :endDate
            GROUP BY c.id, c.name, c.brand, c.location, c.rating, c.pricePerKwh, h.firstName, h.lastName
            ORDER BY COUNT(b) ASC
            """)
    List<Object[]> findUnderutilizedChargers(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Get booking status distribution
     */
    @Query("""
            SELECT b.status, COUNT(b)
            FROM Booking b
            WHERE b.createdAt BETWEEN :startDate AND :endDate
            GROUP BY b.status
            """)
    List<Object[]> getBookingStatusDistribution(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get peak booking hours
     */
    @Query(value = """
            SELECT HOUR(b.start_time) as booking_hour, 
                   COUNT(*) as booking_count
            FROM bookings b
            WHERE b.created_at BETWEEN :startDate AND :endDate
            GROUP BY HOUR(b.start_time)
            ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<Object[]> getPeakBookingHours(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get popular booking durations
     */
    @Query(value = """
            SELECT 
                CASE 
                    WHEN TIMESTAMPDIFF(HOUR, b.start_time, b.end_time) <= 1 THEN '0-1 hours'
                    WHEN TIMESTAMPDIFF(HOUR, b.start_time, b.end_time) <= 2 THEN '1-2 hours'
                    WHEN TIMESTAMPDIFF(HOUR, b.start_time, b.end_time) <= 4 THEN '2-4 hours'
                    WHEN TIMESTAMPDIFF(HOUR, b.start_time, b.end_time) <= 8 THEN '4-8 hours'
                    ELSE '8+ hours'
                END as duration_range,
                COUNT(*) as booking_count
            FROM bookings b
            WHERE b.created_at BETWEEN :startDate AND :endDate
            GROUP BY duration_range
            ORDER BY booking_count DESC
            """, nativeQuery = true)
    List<Object[]> getPopularBookingDurations(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get booking frequency by day of week
     */
    @Query(value = """
            SELECT DAYNAME(b.start_time) as day_of_week, 
                   COUNT(*) as booking_count
            FROM bookings b
            WHERE b.created_at BETWEEN :startDate AND :endDate
            GROUP BY DAYNAME(b.start_time), DAYOFWEEK(b.start_time)
            ORDER BY DAYOFWEEK(b.start_time)
            """, nativeQuery = true)
    List<Object[]> getBookingFrequencyByDay(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get activity heatmap (day + hour)
     */
    @Query(value = """
            SELECT DAYNAME(b.start_time) as day_of_week, 
                   HOUR(b.start_time) as hour,
                   COUNT(*) as activity_count
            FROM bookings b
            WHERE b.created_at BETWEEN :startDate AND :endDate
            GROUP BY DAYNAME(b.start_time), DAYOFWEEK(b.start_time), HOUR(b.start_time)
            ORDER BY DAYOFWEEK(b.start_time), HOUR(b.start_time)
            """, nativeQuery = true)
    List<Object[]> getActivityHeatmap(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get average booking duration
     */
    @Query("""
            SELECT AVG(TIMESTAMPDIFF(HOUR, b.startTime, b.endTime))
            FROM Booking b
            WHERE b.status = 'COMPLETED'
              AND b.createdAt BETWEEN :startDate AND :endDate
            """)
    Double getAverageBookingDuration(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get average lead time (time between booking creation and start time)
     */
    @Query("""
            SELECT AVG(TIMESTAMPDIFF(HOUR, b.createdAt, b.startTime))
            FROM Booking b
            WHERE b.createdAt BETWEEN :startDate AND :endDate
            """)
    Double getAverageLeadTime(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get repeat booking rate
     */
    @Query("""
            SELECT 
                COUNT(DISTINCT CASE WHEN bookingCount > 1 THEN userId END) * 100.0 / COUNT(DISTINCT userId)
            FROM (
                SELECT b.user.userId as userId, COUNT(b) as bookingCount
                FROM Booking b
                WHERE b.createdAt BETWEEN :startDate AND :endDate
                GROUP BY b.user.userId
            ) subquery
            """)
    Double getRepeatBookingRate(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get total energy consumed using aggregation (OPTIMIZED)
     */
    @Query("SELECT COALESCE(SUM(b.totalEnergyKwh), 0.0) FROM Booking b WHERE b.status = :status")
    Double sumEnergyByStatus(@Param("status") BookingStatus status);

    @Query("""
            SELECT COALESCE(SUM(b.totalEnergyKwh), 0.0) 
            FROM Booking b 
            WHERE b.status = :status 
            AND b.createdAt BETWEEN :startDate AND :endDate
            """)
    Double sumEnergyByStatusAndDateRange(
            @Param("status") BookingStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get booking trend data in single query (OPTIMIZED)
     */
    @Query(value = """
            SELECT DATE(b.created_at) as booking_date, COUNT(*) as booking_count
            FROM bookings b
            WHERE b.created_at BETWEEN :startDate AND :endDate
            GROUP BY DATE(b.created_at)
            ORDER BY DATE(b.created_at)
            """, nativeQuery = true)
    List<Object[]> getBookingTrendByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get revenue trend data in single query (OPTIMIZED)
     */
    @Query(value = """
            SELECT DATE(p.created_at) as payment_date, COALESCE(SUM(p.amount), 0) as revenue
            FROM payments p
            WHERE p.status = 'SUCCESS'
            AND p.created_at BETWEEN :startDate AND :endDate
            GROUP BY DATE(p.created_at)
            ORDER BY DATE(p.created_at)
            """, nativeQuery = true)
    List<Object[]> getRevenueTrendByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get user registration trend in single query (OPTIMIZED)
     */
    @Query(value = """
            SELECT DATE(u.created_at) as registration_date, COUNT(*) as user_count
            FROM users u
            WHERE u.created_at BETWEEN :startDate AND :endDate
            GROUP BY DATE(u.created_at)
            ORDER BY DATE(u.created_at)
            """, nativeQuery = true)
    List<Object[]> getUserRegistrationTrend(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}