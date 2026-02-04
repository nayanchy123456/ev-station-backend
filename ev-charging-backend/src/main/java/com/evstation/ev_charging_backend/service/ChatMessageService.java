package com.evstation.ev_charging_backend.service;

import com.evstation.ev_charging_backend.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for chat messaging functionality.
 * 
 * Handles:
 * - Sending and receiving messages
 * - Conversation management
 * - User presence tracking
 * - Message status updates
 */
public interface ChatMessageService {
    
    /**
     * Send a new chat message.
     * 
     * Process:
     * 1. Find or create conversation between sender and receiver
     * 2. Create and save message
     * 3. Update conversation's last message
     * 4. Increment unread count for receiver
     * 5. Return message response for broadcasting
     * 
     * @param senderId ID of the user sending the message
     * @param request Message request containing receiver ID and content
     * @return ChatMessageResponse with full message details
     * @throws ResourceNotFoundException if sender or receiver not found
     */
    ChatMessageResponse sendMessage(Long senderId, ChatMessageRequest request);
    
    /**
     * Get message history for a conversation with pagination.
     * Messages are returned in descending order (newest first).
     * 
     * @param conversationId ID of the conversation
     * @param currentUserId ID of the user requesting history (for authorization)
     * @param pageable Pagination parameters
     * @return Page of ChatMessageResponse objects
     * @throws ResourceNotFoundException if conversation not found
     * @throws UnauthorizedAccessException if user is not part of conversation
     */
    Page<ChatMessageResponse> getConversationHistory(
        Long conversationId, 
        Long currentUserId, 
        Pageable pageable
    );
    
    /**
     * Get all conversations for a user.
     * Returns conversations sorted by last message time (most recent first).
     * Includes unread count and other participant's presence status.
     * 
     * @param userId ID of the user
     * @param pageable Pagination parameters
     * @return Page of ConversationResponse objects
     */
    Page<ConversationResponse> getUserConversations(Long userId, Pageable pageable);
    
    /**
     * Get or create a conversation between two users.
     * If conversation doesn't exist, creates a new one.
     * 
     * @param user1Id First user ID
     * @param user2Id Second user ID
     * @return ConversationResponse with conversation details
     * @throws ResourceNotFoundException if either user not found
     */
    ConversationResponse getOrCreateConversation(Long user1Id, Long user2Id);
    
    /**
     * Mark all messages in a conversation as READ for the current user.
     * Also resets the unread count for that user in the conversation.
     * 
     * @param conversationId ID of the conversation
     * @param userId ID of the user marking messages as read
     * @return Number of messages marked as read
     * @throws ResourceNotFoundException if conversation not found
     */
    int markConversationAsRead(Long conversationId, Long userId);
    
    /**
     * Mark a specific message as READ.
     * 
     * @param messageId ID of the message
     * @param userId ID of the user marking as read (must be receiver)
     * @return Updated ChatMessageResponse
     * @throws ResourceNotFoundException if message not found
     * @throws UnauthorizedAccessException if user is not the receiver
     */
    ChatMessageResponse markMessageAsRead(Long messageId, Long userId);
    
    /**
     * Get unread message count for a conversation.
     * 
     * @param conversationId ID of the conversation
     * @param userId ID of the user
     * @return Number of unread messages
     */
    Long getUnreadCount(Long conversationId, Long userId);
    
    /**
     * Get total unread message count across all conversations for a user.
     * 
     * @param userId ID of the user
     * @return Total unread messages count
     */
    Long getTotalUnreadCount(Long userId);
    
    /**
     * Update user presence to ONLINE.
     * Called when user connects via WebSocket.
     * 
     * @param userId ID of the user
     */
    void setUserOnline(Long userId);
    
    /**
     * Update user presence to OFFLINE.
     * Called when user disconnects from WebSocket.
     * 
     * @param userId ID of the user
     */
    void setUserOffline(Long userId);
    
    /**
     * Get user's current presence status.
     * 
     * @param userId ID of the user
     * @return UserPresenceDto with status and last seen
     */
    UserPresenceDto getUserPresence(Long userId);
    
    /**
     * Mark all undelivered messages for a user as DELIVERED.
     * Called when user comes online.
     * 
     * @param userId ID of the user
     * @return Number of messages marked as delivered
     */
    int markPendingMessagesAsDelivered(Long userId);
    
    /**
     * Search messages in a conversation.
     * 
     * @param conversationId ID of the conversation
     * @param searchTerm Search term (case-insensitive)
     * @param userId ID of the user performing search (for authorization)
     * @param pageable Pagination parameters
     * @return Page of matching messages
     */
    Page<ChatMessageResponse> searchMessages(
        Long conversationId, 
        String searchTerm, 
        Long userId, 
        Pageable pageable
    );
    
    /**
     * Delete a message (soft delete).
     * Message is hidden but not removed from database.
     * 
     * @param messageId ID of the message
     * @param userId ID of the user deleting (must be sender)
     * @throws ResourceNotFoundException if message not found
     * @throws UnauthorizedAccessException if user is not the sender
     */
    void deleteMessage(Long messageId, Long userId);
}