package com.evstation.ev_charging_backend.repository;

import com.evstation.ev_charging_backend.entity.ChatMessage;
import com.evstation.ev_charging_backend.enums.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Enhanced Repository for ChatMessage Entity
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * Get messages in a conversation (excluding deleted messages)
     * Ordered by creation time descending (newest first) for pagination
     * 
     * @param conversationId Conversation ID
     * @param pageable Pagination parameters
     * @return Page of messages
     */
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "m.conversation.id = :conversationId AND " +
           "m.isDeleted = false " +
           "ORDER BY m.createdAt DESC")
    Page<ChatMessage> findByConversationIdAndIsDeletedFalse(
        @Param("conversationId") Long conversationId,
        Pageable pageable
    );
    
    /**
     * Get unread messages for a user in a conversation
     * 
     * @param conversationId Conversation ID
     * @param userId User ID (receiver)
     * @return List of unread messages
     */
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "m.conversation.id = :conversationId AND " +
           "m.receiver.userId = :userId AND " +
           "m.status != 'READ' AND " +
           "m.isDeleted = false " +
           "ORDER BY m.createdAt ASC")
    List<ChatMessage> findUnreadMessagesForUser(
        @Param("conversationId") Long conversationId,
        @Param("userId") Long userId
    );
    
    /**
     * Count unread messages for a user in a conversation
     * 
     * @param conversationId Conversation ID
     * @param userId User ID (receiver)
     * @return Count of unread messages
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE " +
           "m.conversation.id = :conversationId AND " +
           "m.receiver.userId = :userId AND " +
           "m.status != 'READ' AND " +
           "m.isDeleted = false")
    Long countUnreadMessages(
        @Param("conversationId") Long conversationId,
        @Param("userId") Long userId
    );
    
    /**
     * Find pending messages (SENT but not DELIVERED) for a user
     * Used when user comes online to mark messages as delivered
     * 
     * @param receiverId Receiver user ID
     * @param status Message status (typically SENT)
     * @return List of pending messages
     */
    List<ChatMessage> findByReceiverUserIdAndStatusAndIsDeletedFalse(
        Long receiverId,
        MessageStatus status
    );
    
    /**
     * Search messages in a conversation by content
     * 
     * @param conversationId Conversation ID
     * @param searchTerm Search term
     * @param pageable Pagination
     * @return Page of matching messages
     */
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "m.conversation.id = :conversationId AND " +
           "m.isDeleted = false AND " +
           "LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY m.createdAt DESC")
    Page<ChatMessage> searchInConversation(
        @Param("conversationId") Long conversationId,
        @Param("searchTerm") String searchTerm,
        Pageable pageable
    );
    
    /**
     * Get last message in a conversation
     * 
     * @param conversationId Conversation ID
     * @return Last message or null
     */
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "m.conversation.id = :conversationId AND " +
           "m.isDeleted = false " +
           "ORDER BY m.createdAt DESC")
    List<ChatMessage> findLastMessage(@Param("conversationId") Long conversationId, Pageable pageable);
    
    /**
     * Count total messages in a conversation
     * 
     * @param conversationId Conversation ID
     * @return Message count
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE " +
           "m.conversation.id = :conversationId AND " +
           "m.isDeleted = false")
    Long countMessagesByConversationId(@Param("conversationId") Long conversationId);
    
    /**
     * Get messages sent by a specific user
     * 
     * @param userId User ID
     * @param pageable Pagination
     * @return Page of messages
     */
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "m.sender.userId = :userId AND " +
           "m.isDeleted = false " +
           "ORDER BY m.createdAt DESC")
    Page<ChatMessage> findBySenderId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Get messages received by a specific user
     * 
     * @param userId User ID
     * @param pageable Pagination
     * @return Page of messages
     */
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "m.receiver.userId = :userId AND " +
           "m.isDeleted = false " +
           "ORDER BY m.createdAt DESC")
    Page<ChatMessage> findByReceiverId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Delete all messages in a conversation (soft delete)
     * 
     * @param conversationId Conversation ID
     * @return Number of messages deleted
     */
    @Query("UPDATE ChatMessage m SET m.isDeleted = true WHERE m.conversation.id = :conversationId")
    int softDeleteAllByConversationId(@Param("conversationId") Long conversationId);
}