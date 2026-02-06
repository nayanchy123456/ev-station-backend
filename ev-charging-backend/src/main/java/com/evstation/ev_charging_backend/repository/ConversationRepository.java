package com.evstation.ev_charging_backend.repository;

import com.evstation.ev_charging_backend.entity.Conversation;
import com.evstation.ev_charging_backend.enums.ConversationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Enhanced Repository for Conversation Entity
 * 
 * Provides methods to:
 * - Find conversations by participants
 * - Filter conversations by type (USER_HOST, USER_ADMIN, HOST_ADMIN)
 * - Search conversations
 * - Get admin support conversations
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    /**
     * Find conversation between two users (bidirectional)
     * Used by BookingServiceImpl for auto-conversation creation
     * 
     * @param user1Id First user ID
     * @param user2Id Second user ID
     * @return Optional conversation
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.user1Id = :user1Id AND c.user2Id = :user2Id) OR " +
           "(c.user1Id = :user2Id AND c.user2Id = :user1Id)")
    Optional<Conversation> findByUsers(
        @Param("user1Id") Long user1Id,
        @Param("user2Id") Long user2Id
    );
    
    /**
     * Find conversation between two users with specific type and context
     * Used for finding existing conversation or determining if new one is needed
     * 
     * @param user1Id First user ID
     * @param user2Id Second user ID
     * @param type Conversation type
     * @param chargerId Charger ID (can be null for non-charger conversations)
     * @return Optional conversation
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "((c.user1Id = :user1Id AND c.user2Id = :user2Id) OR " +
           " (c.user1Id = :user2Id AND c.user2Id = :user1Id)) AND " +
           "c.conversationType = :type AND " +
           "(:chargerId IS NULL OR c.chargerId = :chargerId)")
    Optional<Conversation> findByParticipantsAndTypeAndCharger(
        @Param("user1Id") Long user1Id,
        @Param("user2Id") Long user2Id,
        @Param("type") ConversationType type,
        @Param("chargerId") Long chargerId
    );
    
    /**
     * Find conversation between two users (any type)
     * Useful for general conversation lookup
     * 
     * @param user1Id First user ID
     * @param user2Id Second user ID
     * @return List of conversations (could be multiple with different types)
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.user1Id = :user1Id AND c.user2Id = :user2Id) OR " +
           "(c.user1Id = :user2Id AND c.user2Id = :user1Id)")
    List<Conversation> findByParticipants(
        @Param("user1Id") Long user1Id,
        @Param("user2Id") Long user2Id
    );
    
    /**
     * Get all conversations for a specific user, ordered by last message time
     * Excludes archived conversations by default
     * 
     * @param userId User ID
     * @param pageable Pagination parameters
     * @return Page of conversations
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.user1Id = :userId OR c.user2Id = :userId) AND " +
           "c.isActive = true AND " +
           "((c.user1Id = :userId AND c.archivedUser1 = false) OR " +
           " (c.user2Id = :userId AND c.archivedUser2 = false)) " +
           "ORDER BY c.lastMessageTime DESC NULLS LAST, c.createdAt DESC")
    Page<Conversation> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Get all conversations for a user filtered by type
     * 
     * @param userId User ID
     * @param type Conversation type
     * @param pageable Pagination parameters
     * @return Page of conversations
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.user1Id = :userId OR c.user2Id = :userId) AND " +
           "c.conversationType = :type AND " +
           "c.isActive = true AND " +
           "((c.user1Id = :userId AND c.archivedUser1 = false) OR " +
           " (c.user2Id = :userId AND c.archivedUser2 = false)) " +
           "ORDER BY c.lastMessageTime DESC NULLS LAST, c.createdAt DESC")
    Page<Conversation> findByUserIdAndType(
        @Param("userId") Long userId,
        @Param("type") ConversationType type,
        Pageable pageable
    );
    
    /**
     * Get all support conversations where user is talking to admin
     * Useful for admin dashboard to see all support requests
     * 
     * @param adminId Admin user ID
     * @param pageable Pagination parameters
     * @return Page of support conversations
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.user1Id = :adminId OR c.user2Id = :adminId) AND " +
           "(c.conversationType = 'USER_ADMIN' OR c.conversationType = 'HOST_ADMIN') AND " +
           "c.isActive = true " +
           "ORDER BY c.lastMessageTime DESC NULLS LAST, c.createdAt DESC")
    Page<Conversation> findAdminSupportConversations(
        @Param("adminId") Long adminId,
        Pageable pageable
    );
    
    /**
     * Get all conversations related to a specific charger
     * Useful for hosts to see all user inquiries about their charger
     * 
     * @param chargerId Charger ID
     * @param pageable Pagination parameters
     * @return Page of conversations
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "c.chargerId = :chargerId AND " +
           "c.conversationType = 'USER_HOST' AND " +
           "c.isActive = true " +
           "ORDER BY c.lastMessageTime DESC NULLS LAST, c.createdAt DESC")
    Page<Conversation> findByChargerId(@Param("chargerId") Long chargerId, Pageable pageable);
    
    /**
     * Count unread conversations for a user
     * 
     * @param userId User ID
     * @return Number of conversations with unread messages
     */
    @Query("SELECT COUNT(c) FROM Conversation c WHERE " +
           "((c.user1Id = :userId AND c.unreadCountUser1 > 0) OR " +
           " (c.user2Id = :userId AND c.unreadCountUser2 > 0)) AND " +
           "c.isActive = true")
    Long countUnreadConversations(@Param("userId") Long userId);
    
    /**
     * Get total unread message count for a user across all conversations
     * 
     * @param userId User ID
     * @return Total unread message count
     */
    @Query("SELECT COALESCE(SUM(CASE " +
           "WHEN c.user1Id = :userId THEN c.unreadCountUser1 " +
           "WHEN c.user2Id = :userId THEN c.unreadCountUser2 " +
           "ELSE 0 END), 0) " +
           "FROM Conversation c WHERE " +
           "(c.user1Id = :userId OR c.user2Id = :userId) AND " +
           "c.isActive = true")
    Long getTotalUnreadCount(@Param("userId") Long userId);
    
    /**
     * Search conversations by participant name or last message
     * 
     * @param userId Current user ID
     * @param searchTerm Search term
     * @param pageable Pagination parameters
     * @return Page of matching conversations
     */
    @Query("SELECT DISTINCT c FROM Conversation c " +
           "LEFT JOIN User u1 ON c.user1Id = u1.userId " +
           "LEFT JOIN User u2 ON c.user2Id = u2.userId " +
           "WHERE (c.user1Id = :userId OR c.user2Id = :userId) AND " +
           "c.isActive = true AND " +
           "(LOWER(c.lastMessage) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           " LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           " LOWER(u1.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           " LOWER(u1.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           " LOWER(u2.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           " LOWER(u2.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY c.lastMessageTime DESC NULLS LAST")
    Page<Conversation> searchConversations(
        @Param("userId") Long userId,
        @Param("searchTerm") String searchTerm,
        Pageable pageable
    );
    
    /**
     * Get archived conversations for a user
     * 
     * @param userId User ID
     * @param pageable Pagination parameters
     * @return Page of archived conversations
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.user1Id = :userId OR c.user2Id = :userId) AND " +
           "c.isActive = true AND " +
           "((c.user1Id = :userId AND c.archivedUser1 = true) OR " +
           " (c.user2Id = :userId AND c.archivedUser2 = true)) " +
           "ORDER BY c.lastMessageTime DESC NULLS LAST")
    Page<Conversation> findArchivedConversations(@Param("userId") Long userId, Pageable pageable);
}