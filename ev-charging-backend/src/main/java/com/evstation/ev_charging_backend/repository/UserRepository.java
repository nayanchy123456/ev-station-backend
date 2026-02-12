package com.evstation.ev_charging_backend.repository;

import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // ============================================
    // BASIC USER QUERIES
    // ============================================

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    // ✅ Fetch users by role (e.g., PENDING_HOST)
    List<User> findByRole(Role role);
    
    // ✅ Find users by role excluding current user (for admin chat)
    Page<User> findByRoleAndUserIdNot(Role role, Long userId, Pageable pageable);
    
    // ✅ Find all users excluding current user (for admin chat)
    Page<User> findByUserIdNot(Long userId, Pageable pageable);
    
    // ✅ Search users by name/email/phone with role filter
    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           " LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           " u.phone LIKE CONCAT('%', :searchTerm, '%')) AND " +
           "u.role = :role AND u.userId != :excludeUserId")
    Page<User> searchByTermAndRole(
        @Param("searchTerm") String searchTerm, 
        @Param("role") Role role,
        @Param("excludeUserId") Long excludeUserId,
        Pageable pageable
    );
    
    // ✅ Search users by name/email/phone (all roles)
    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           " LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           " u.phone LIKE CONCAT('%', :searchTerm, '%')) AND " +
           "u.userId != :excludeUserId")
    Page<User> searchByTerm(
        @Param("searchTerm") String searchTerm,
        @Param("excludeUserId") Long excludeUserId,
        Pageable pageable
    );

    // ============================================
    // USER ANALYTICS QUERIES
    // ============================================

    /**
     * Get daily user registration trend
     * Returns: [date, count]
     */
    @Query(value = """
            SELECT DATE(u.created_at) as date,
                   COUNT(*) as user_count
            FROM users u
            WHERE u.created_at BETWEEN :startDate AND :endDate
            GROUP BY DATE(u.created_at)
            ORDER BY DATE(u.created_at)
            """, nativeQuery = true)
    List<Object[]> getDailyUserRegistrationTrend(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count users by role
     */
    Long countByRole(Role role);
    
    /**
     * Count users created after a specific date
     */
    Long countByCreatedAtAfter(LocalDateTime date);
    
    /**
     * Count users created within a date range
     */
    Long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Count users created before a specific date
     */
    Long countByCreatedAtBefore(LocalDateTime date);
    
    /**
     * Count users by role within a date range
     */
    Long countByRoleAndCreatedAtBetween(Role role, LocalDateTime start, LocalDateTime end);
    
    /**
     * Count total users who have made at least one booking
     */
    @Query("SELECT COUNT(DISTINCT b.user.userId) FROM Booking b")
    Long countUsersWithBookings();
    
    /**
     * Count users who have made bookings after a specific date
     */
    @Query("SELECT COUNT(DISTINCT b.user.userId) FROM Booking b WHERE b.createdAt > :date")
    Long countUsersWithBookingsAfter(@Param("date") LocalDateTime date);
}