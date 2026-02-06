package com.evstation.ev_charging_backend.repository;

import com.evstation.ev_charging_backend.entity.UserPresence;
import com.evstation.ev_charging_backend.enums.UserPresenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing UserPresence entities.
 * 
 * Key Features:
 * - Track user online/offline status
 * - Update presence on connect/disconnect
 * - Query users by status
 */
@Repository
public interface UserPresenceRepository extends JpaRepository<UserPresence, Long> {
    
    /**
     * Find presence record by user ID.
     * 
     * @param userId The user's ID
     * @return Optional containing UserPresence if found
     */
    @Query("SELECT p FROM UserPresence p WHERE p.user.userId = :userId")
    Optional<UserPresence> findByUserId(@Param("userId") Long userId);
    
    /**
     * Check if a user is currently online.
     * 
     * @param userId The user's ID
     * @return true if user is online, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM UserPresence p WHERE " +
           "p.user.userId = :userId AND p.status = 'ONLINE'")
    boolean isUserOnline(@Param("userId") Long userId);
    
    /**
     * Find all online users.
     * Useful for admin monitoring or displaying online users list.
     * 
     * @return List of all online user presence records
     */
    @Query("SELECT p FROM UserPresence p WHERE p.status = 'ONLINE'")
    List<UserPresence> findAllOnlineUsers();
    
    /**
     * Count total online users.
     * 
     * @return Number of users currently online
     */
    @Query("SELECT COUNT(p) FROM UserPresence p WHERE p.status = 'ONLINE'")
    Long countOnlineUsers();
    
    /**
     * Update user status to ONLINE.
     * 
     * @param userId The user's ID
     * @return Number of records updated (should be 0 or 1)
     */
    @Modifying
    @Query("UPDATE UserPresence p SET " +
           "p.status = 'ONLINE', " +
           "p.lastSeenAt = CURRENT_TIMESTAMP " +
           "WHERE p.user.userId = :userId")
    int setUserOnline(@Param("userId") Long userId);
    
    /**
     * Update user status to OFFLINE.
     * 
     * @param userId The user's ID
     * @return Number of records updated (should be 0 or 1)
     */
    @Modifying
    @Query("UPDATE UserPresence p SET " +
           "p.status = 'OFFLINE', " +
           "p.lastSeenAt = CURRENT_TIMESTAMP " +
           "WHERE p.user.userId = :userId")
    int setUserOffline(@Param("userId") Long userId);
    
    /**
     * Find presence status for multiple users at once.
     * Useful for checking status of all conversation participants.
     * 
     * @param userIds List of user IDs
     * @return List of presence records
     */
    @Query("SELECT p FROM UserPresence p WHERE p.user.userId IN :userIds")
    List<UserPresence> findByUserIds(@Param("userIds") List<Long> userIds);
    
    /**
     * Delete presence record for a user.
     * Useful for cleanup when user account is deleted.
     * 
     * @param userId The user's ID
     */
    @Modifying
    @Query("DELETE FROM UserPresence p WHERE p.user.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}