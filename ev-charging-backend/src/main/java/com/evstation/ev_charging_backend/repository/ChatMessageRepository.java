package com.evstation.ev_charging_backend.repository;

import com.evstation.ev_charging_backend.entity.ChatMessage;
import com.evstation.ev_charging_backend.enums.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing ChatMessage entities.
 * 
 * Key Features:
 * - Retrieve messages by conversation
 * - Mark messages as read/delivered
 * - Count unread messages
 * - Search messages
 * 
 * 🔧 FIXED: Changed findLatestMessage to use Optional instead of invalid LIMIT clause
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * Find all messages in a conversation, ordered by creation time (newest first).
     * Excludes soft-deleted messages.
     * 
     * @param conversationId The conversation ID
     * @param pageable Pagination parameters
     * @return Page of messages
     */
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "m.conversation.id = :conversationId AND " +
           "m.isDeleted = false " +
           "ORDER BY m.createdAt DESC")
    Page<ChatMessage> findByConversationId(
        @Param("conversationId") Long conversationId,
        Pageable pageable
    );
    
    /**
     * Count unread messages in a conversation for a specific user.
     * 
     * @param conversationId The conversation ID
     * @param receiverId The receiver's user ID
     * @return Count of unread messages
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE " +
           "m.conversation.id = :conversationId AND " +
           "m.receiver.userId = :receiverId AND " +
           "m.status != 'READ' AND " +
           "m.isDeleted = false")
    Long countUnreadMessages(
        @Param("conversationId") Long conversationId,
        @Param("receiverId") Long receiverId
    );
    
    /**
     * Mark all messages in a conversation as READ for a specific receiver.
     * Used when user opens a conversation.
     * 
     * @param conversationId The conversation ID
     * @param receiverId The receiver's user ID
     * @return Number of messages updated
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET " +
           "m.status = 'READ', " +
           "m.readAt = CURRENT_TIMESTAMP " +
           "WHERE m.conversation.id = :conversationId AND " +
           "m.receiver.userId = :receiverId AND " +
           "m.status != 'READ' AND " +
           "m.isDeleted = false")
    int markAllAsRead(
        @Param("conversationId") Long conversationId,
        @Param("receiverId") Long receiverId
    );
    
    /**
     * Mark a specific message as READ.
     * 
     * @param messageId The message ID
     * @return Number of messages updated (should be 0 or 1)
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET " +
           "m.status = 'READ', " +
           "m.readAt = CURRENT_TIMESTAMP " +
           "WHERE m.id = :messageId AND m.status != 'READ'")
    int markAsRead(@Param("messageId") Long messageId);
    
    /**
     * Mark messages as DELIVERED for a specific receiver.
     * Used when user comes online and receives pending messages.
     * 
     * @param receiverId The receiver's user ID
     * @return Number of messages updated
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET " +
           "m.status = 'DELIVERED', " +
           "m.deliveredAt = CURRENT_TIMESTAMP " +
           "WHERE m.receiver.userId = :receiverId AND " +
           "m.status = 'SENT' AND " +
           "m.isDeleted = false")
    int markAsDelivered(@Param("receiverId") Long receiverId);
    
    /**
     * Find unread messages for a user across all conversations.
     * 
     * @param receiverId The receiver's user ID
     * @return List of unread messages
     */
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "m.receiver.userId = :receiverId AND " +
           "m.status != 'READ' AND " +
           "m.isDeleted = false " +
           "ORDER BY m.createdAt DESC")
    List<ChatMessage> findUnreadMessages(@Param("receiverId") Long receiverId);
    
    /**
     * Search messages in a conversation by content.
     * Case-insensitive search.
     * 
     * @param conversationId The conversation ID
     * @param searchTerm The search term
     * @param pageable Pagination parameters
     * @return Page of matching messages
     */
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "m.conversation.id = :conversationId AND " +
           "LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND " +
           "m.isDeleted = false " +
           "ORDER BY m.createdAt DESC")
    Page<ChatMessage> searchInConversation(
        @Param("conversationId") Long conversationId,
        @Param("searchTerm") String searchTerm,
        Pageable pageable
    );
    
    /**
     * 🔧 FIXED: Get the latest message in a conversation.
     * Changed from invalid LIMIT clause to using First naming convention.
     * 
     * @param conversationId The conversation ID
     * @return Optional containing the most recent message if exists
     */
    Optional<ChatMessage> findFirstByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(
        Long conversationId
    );
    
    /**
     * Count total messages sent by a user.
     * 
     * @param userId The user's ID
     * @return Total message count
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE " +
           "m.sender.userId = :userId AND m.isDeleted = false")
    Long countByUserId(@Param("userId") Long userId);
}