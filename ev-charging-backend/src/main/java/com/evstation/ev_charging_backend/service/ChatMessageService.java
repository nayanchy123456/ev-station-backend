package com.evstation.ev_charging_backend.service;

import com.evstation.ev_charging_backend.dto.*;
import com.evstation.ev_charging_backend.enums.ConversationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Enhanced Chat Message Service Interface
 * 
 * Supports:
 * - Different conversation types (USER_HOST, USER_ADMIN, HOST_ADMIN)
 * - Real-time messaging via WebSocket
 * - Message status tracking (sent, delivered, read)
 * - Typing indicators
 * - User presence
 * - Conversation management
 * - Admin chat functionality
 */
public interface ChatMessageService {
    
    // ==================== MESSAGE OPERATIONS ====================
    
    /**
     * Send a message in an existing or new conversation
     * 
     * @param senderId ID of the sender
     * @param request Message request containing receiver and content
     * @return ChatMessageResponse with delivery status
     */
    ChatMessageResponse sendMessage(Long senderId, ChatMessageRequest request);
    
    /**
     * Get messages in a conversation with pagination
     * 
     * @param conversationId Conversation ID
     * @param userId Current user ID (for authorization)
     * @param pageable Pagination parameters
     * @return Page of messages
     */
    Page<ChatMessageResponse> getConversationMessages(
        Long conversationId,
        Long userId,
        Pageable pageable
    );
    
    /**
     * Mark a specific message as read
     * 
     * @param messageId Message ID
     * @param userId User marking the message as read
     * @return Updated message
     */
    ChatMessageResponse markMessageAsRead(Long messageId, Long userId);
    
    /**
     * Mark all messages in a conversation as read
     * 
     * @param conversationId Conversation ID
     * @param userId User marking messages as read
     * @return Number of messages marked as read
     */
    Integer markConversationAsRead(Long conversationId, Long userId);
    
    /**
     * Delete a message (soft delete)
     * 
     * @param messageId Message ID
     * @param userId User requesting deletion (must be sender)
     */
    void deleteMessage(Long messageId, Long userId);
    
    /**
     * Search messages within a conversation
     * 
     * @param conversationId Conversation ID
     * @param searchTerm Search term
     * @param userId Current user ID
     * @param pageable Pagination
     * @return Page of matching messages
     */
    Page<ChatMessageResponse> searchMessages(
        Long conversationId,
        String searchTerm,
        Long userId,
        Pageable pageable
    );
    
    // ==================== CONVERSATION OPERATIONS ====================
    
    /**
     * Initiate or get a conversation with context
     * 
     * @param currentUserId Current user ID
     * @param request Conversation initiation request
     * @return Conversation response
     */
    ConversationResponse initiateConversation(
        Long currentUserId,
        ConversationInitiateRequest request
    );
    
    /**
     * Get or create conversation between two users with specific type
     * 
     * @param user1Id First user ID
     * @param user2Id Second user ID
     * @param type Conversation type
     * @param chargerId Charger ID (for USER_HOST type)
     * @return Conversation response
     */
    ConversationResponse getOrCreateConversation(
        Long user1Id,
        Long user2Id,
        ConversationType type,
        Long chargerId
    );
    
    /**
     * Get all conversations for a user
     * 
     * @param userId User ID
     * @param pageable Pagination
     * @return Page of conversations
     */
    Page<ConversationResponse> getUserConversations(Long userId, Pageable pageable);
    
    /**
     * Get conversations filtered by type
     * 
     * @param userId User ID
     * @param type Conversation type
     * @param pageable Pagination
     * @return Page of conversations
     */
    Page<ConversationResponse> getUserConversationsByType(
        Long userId,
        ConversationType type,
        Pageable pageable
    );
    
    /**
     * Get a specific conversation by ID
     * 
     * @param conversationId Conversation ID
     * @param userId Current user ID (for authorization)
     * @return Conversation details
     */
    ConversationResponse getConversationById(Long conversationId, Long userId);
    
    /**
     * Archive/Unarchive a conversation
     * 
     * @param conversationId Conversation ID
     * @param userId User performing the action
     * @param archive true to archive, false to unarchive
     */
    void toggleArchiveConversation(Long conversationId, Long userId, boolean archive);
    
    /**
     * Search conversations
     * 
     * @param userId Current user ID
     * @param searchTerm Search term
     * @param pageable Pagination
     * @return Page of matching conversations
     */
    Page<ConversationResponse> searchConversations(
        Long userId,
        String searchTerm,
        Pageable pageable
    );
    
    // ==================== ADMIN OPERATIONS ====================
    
    /**
     * Get all support conversations for admin
     * 
     * @param adminId Admin user ID
     * @param pageable Pagination
     * @return Page of support conversations
     */
    Page<ConversationResponse> getAdminSupportConversations(
        Long adminId,
        Pageable pageable
    );
    
    /**
     * Search users/hosts for admin to initiate chat
     * 
     * @param adminId Admin user ID
     * @param request Search request
     * @return Page of users matching search criteria
     */
    Page<UserSearchResponse> searchUsersForAdminChat(
        Long adminId,
        AdminChatSearchRequest request
    );
    
    /**
     * Admin initiates chat with a user or host
     * 
     * @param adminId Admin user ID
     * @param targetUserId Target user/host ID
     * @param initialMessage Optional initial message
     * @return Created conversation
     */
    ConversationResponse adminInitiateChat(
        Long adminId,
        Long targetUserId,
        String initialMessage
    );
    
    // ==================== CHARGER-SPECIFIC OPERATIONS ====================
    
    /**
     * Get host ID for a specific charger
     * 
     * @param chargerId Charger ID
     * @return Host user ID
     */
    Long getChargerHostId(Long chargerId);
    
    /**
     * Get conversations related to a specific charger
     * 
     * @param chargerId Charger ID
     * @param hostId Host user ID (for authorization)
     * @param pageable Pagination
     * @return Page of conversations about this charger
     */
    Page<ConversationResponse> getChargerConversations(
        Long chargerId,
        Long hostId,
        Pageable pageable
    );
    
    // ==================== PRESENCE & STATUS ====================
    
    /**
     * Get user presence information
     * 
     * @param userId User ID
     * @return User presence DTO
     */
    UserPresenceDto getUserPresence(Long userId);
    
    /**
     * Update user presence status
     * 
     * @param userId User ID
     * @param isOnline Online status
     */
    void updateUserPresence(Long userId, boolean isOnline);
    
    /**
     * Set user as online (called by WebSocket connection)
     * 
     * @param userId User ID
     */
    void setUserOnline(Long userId);
    
    /**
     * Set user as offline (called by WebSocket disconnection)
     * 
     * @param userId User ID
     */
    void setUserOffline(Long userId);
    
    /**
     * Mark pending messages as delivered when user comes online
     * 
     * @param userId User ID who just came online
     * @return Number of messages marked as delivered
     */
    int markPendingMessagesAsDelivered(Long userId);
    
    // ==================== UNREAD COUNT ====================
    
    /**
     * Get unread count for a specific conversation
     * 
     * @param conversationId Conversation ID
     * @param userId Current user ID
     * @return Unread message count
     */
    Long getUnreadCount(Long conversationId, Long userId);
    
    /**
     * Get total unread count across all conversations
     * 
     * @param userId User ID
     * @return Total unread count
     */
    Long getTotalUnreadCount(Long userId);
}