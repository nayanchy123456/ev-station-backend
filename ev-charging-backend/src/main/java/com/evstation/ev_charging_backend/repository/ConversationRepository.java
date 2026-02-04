package com.evstation.ev_charging_backend.repository;

import com.evstation.ev_charging_backend.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing Conversation entities.
 * 
 * Key Features:
 * - Find conversation between two users
 * - Get all conversations for a user
 * - Check if conversation exists
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    /**
     * Find a conversation between two users (order-independent).
     * Works regardless of which user ID is passed first.
     * 
     * Example:
     * findByUsers(3, 5) will find the same conversation as findByUsers(5, 3)
     * 
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return Optional containing the conversation if found
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.user1Id = :userId1 AND c.user2Id = :userId2) OR " +
           "(c.user1Id = :userId2 AND c.user2Id = :userId1)")
    Optional<Conversation> findByUsers(
        @Param("userId1") Long userId1, 
        @Param("userId2") Long userId2
    );
    
    /**
     * Find all conversations involving a specific user.
     * Returns conversations sorted by last message time (most recent first).
     * 
     * @param userId The user's ID
     * @param pageable Pagination parameters
     * @return Page of conversations
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "c.user1Id = :userId OR c.user2Id = :userId " +
           "ORDER BY c.lastMessageTime DESC NULLS LAST")
    Page<Conversation> findByUserId(
        @Param("userId") Long userId,
        Pageable pageable
    );
    
    /**
     * Count total conversations for a user.
     * 
     * @param userId The user's ID
     * @return Total number of conversations
     */
    @Query("SELECT COUNT(c) FROM Conversation c WHERE " +
           "c.user1Id = :userId OR c.user2Id = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    /**
     * Check if a conversation exists between two users.
     * 
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return true if conversation exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM Conversation c WHERE " +
           "(c.user1Id = :userId1 AND c.user2Id = :userId2) OR " +
           "(c.user1Id = :userId2 AND c.user2Id = :userId1)")
    boolean existsByUsers(
        @Param("userId1") Long userId1,
        @Param("userId2") Long userId2
    );
    
    /**
     * Get total unread count across all conversations for a user.
     * Useful for showing notification badge.
     * 
     * @param userId The user's ID
     * @return Total unread messages count
     */
    @Query("SELECT COALESCE(SUM(CASE " +
           "WHEN c.user1Id = :userId THEN c.unreadCountUser1 " +
           "WHEN c.user2Id = :userId THEN c.unreadCountUser2 " +
           "ELSE 0 END), 0) " +
           "FROM Conversation c WHERE c.user1Id = :userId OR c.user2Id = :userId")
    Long getTotalUnreadCount(@Param("userId") Long userId);
}