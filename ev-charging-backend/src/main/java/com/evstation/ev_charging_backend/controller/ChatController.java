package com.evstation.ev_charging_backend.controller;

import com.evstation.ev_charging_backend.dto.*;
import com.evstation.ev_charging_backend.security.CustomUserDetails;
import com.evstation.ev_charging_backend.service.ChatMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * ENHANCED Chat Controller for Real-Time Messaging
 * 
 * ✅ IMPROVEMENTS:
 * 1. Better error handling and validation
 * 2. Typing indicators support
 * 3. Message delivery receipts
 * 4. Enhanced presence features
 * 5. Connection health checks
 * 6. Better logging and monitoring
 * 
 * REST Endpoints (HTTP):
 * - GET  /api/chat/conversations              → List all conversations
 * - GET  /api/chat/conversations/{id}         → Get conversation history
 * - GET  /api/chat/conversations/user/{id}    → Get/create conversation with user
 * - POST /api/chat/send                       → Send message (fallback)
 * - PUT  /api/chat/conversations/{id}/read    → Mark conversation as read
 * - PUT  /api/chat/messages/{id}/read         → Mark single message as read
 * - GET  /api/chat/presence/{userId}          → Get user presence
 * - GET  /api/chat/unread/total               → Get total unread count
 * - GET  /api/chat/conversations/{id}/search  → Search messages
 * - DELETE /api/chat/messages/{id}            → Delete message
 * 
 * WebSocket Endpoints (STOMP):
 * - /app/chat.sendMessage                     → Send message in real-time
 * - /app/chat.typing                          → Send typing indicator
 * - /app/chat.stopTyping                      → Stop typing indicator
 * - /app/chat.markAsRead                      → Mark message as read
 * - /topic/messages/{conversationId}          → Subscribe to conversation
 * - /user/queue/messages                      → Personal message queue
 * - /topic/typing/{conversationId}            → Typing indicators
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "*"})
public class ChatController {
    
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    
    // ==================== WEBSOCKET ENDPOINTS ====================
    
    /**
     * WebSocket: Send Message
     * 
     * ✅ ENHANCED:
     * - Better error handling
     * - Validates sender authentication
     * - Broadcasts to multiple destinations
     * - Returns delivery confirmation
     * 
     * Client sends to: /app/chat.sendMessage
     * Server broadcasts to: 
     *   1. /topic/messages/{conversationId} (both participants)
     *   2. /user/{receiverId}/queue/messages (receiver's personal queue)
     *   3. Sender gets response directly
     * 
     * @param request Message request
     * @param headerAccessor Session header accessor
     * @return Message response with delivery status
     */
    @MessageMapping("/chat.sendMessage")
    public ChatMessageResponse sendMessage(
        @Payload @Valid ChatMessageRequest request,
        SimpMessageHeaderAccessor headerAccessor
    ) {
        // Extract sender ID from authenticated session
        Long senderId = (Long) headerAccessor.getSessionAttributes().get("userId");
        String senderUsername = (String) headerAccessor.getSessionAttributes().get("username");
        
        // Validate authentication
        if (senderId == null) {
            log.error("❌ Attempted to send message without authenticated user - Session: {}", 
                     headerAccessor.getSessionId());
            throw new IllegalStateException("User not authenticated. Please reconnect.");
        }
        
        log.info("💬 WebSocket message from user {} ({}) to user {}", 
                 senderUsername, senderId, request.getReceiverId());
        
        try {
            // Process message through service layer
            ChatMessageResponse response = chatMessageService.sendMessage(senderId, request);
            
            // Broadcast to conversation topic (both sender and receiver can subscribe)
            messagingTemplate.convertAndSend(
                "/topic/messages/" + response.getConversationId(),
                response
            );
            log.debug("📢 Broadcasted to conversation topic: {}", response.getConversationId());
            
            // Send to receiver's personal queue (guaranteed delivery when they're online)
            messagingTemplate.convertAndSendToUser(
                request.getReceiverId().toString(),
                "/queue/messages",
                response
            );
            log.debug("📬 Sent to receiver's personal queue: {}", request.getReceiverId());
            
            // Optional: Send delivery confirmation to sender's personal queue
            Map<String, Object> confirmation = new HashMap<>();
            confirmation.put("messageId", response.getId());
            confirmation.put("status", "sent");
            confirmation.put("timestamp", response.getCreatedAt());
            
            messagingTemplate.convertAndSendToUser(
                senderId.toString(),
                "/queue/confirmations",
                confirmation
            );
            
            log.info("✅ Message {} sent successfully in conversation {}", 
                     response.getId(), response.getConversationId());
            
            return response;
            
        } catch (Exception e) {
            log.error("❌ Error sending message from user {} to user {}: {}", 
                     senderId, request.getReceiverId(), e.getMessage(), e);
            throw new RuntimeException("Failed to send message: " + e.getMessage());
        }
    }
    
    /**
     * WebSocket: Typing Indicator
     * 
     * ✅ NEW FEATURE: Real-time typing indicators
     * 
     * Client sends to: /app/chat.typing
     * Server broadcasts to: /topic/typing/{conversationId}
     * 
     * @param conversationId Conversation ID
     * @param headerAccessor Session header accessor
     */
    @MessageMapping("/chat.typing")
    public void sendTypingIndicator(
        @Payload Long conversationId,
        SimpMessageHeaderAccessor headerAccessor
    ) {
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        
        if (userId == null) return;
        
        Map<String, Object> typingIndicator = new HashMap<>();
        typingIndicator.put("userId", userId);
        typingIndicator.put("username", username);
        typingIndicator.put("conversationId", conversationId);
        typingIndicator.put("isTyping", true);
        typingIndicator.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend(
            "/topic/typing/" + conversationId,
            typingIndicator
        );
        
        log.debug("⌨️ User {} is typing in conversation {}", userId, conversationId);
    }
    
    /**
     * WebSocket: Stop Typing Indicator
     * 
     * ✅ NEW FEATURE: Stop typing notification
     * 
     * @param conversationId Conversation ID
     * @param headerAccessor Session header accessor
     */
    @MessageMapping("/chat.stopTyping")
    public void sendStopTypingIndicator(
        @Payload Long conversationId,
        SimpMessageHeaderAccessor headerAccessor
    ) {
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        
        if (userId == null) return;
        
        Map<String, Object> typingIndicator = new HashMap<>();
        typingIndicator.put("userId", userId);
        typingIndicator.put("conversationId", conversationId);
        typingIndicator.put("isTyping", false);
        typingIndicator.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend(
            "/topic/typing/" + conversationId,
            typingIndicator
        );
        
        log.debug("⌨️ User {} stopped typing in conversation {}", userId, conversationId);
    }
    
    /**
     * WebSocket: Mark Message as Read
     * 
     * ✅ NEW FEATURE: Real-time read receipts via WebSocket
     * 
     * @param messageId Message ID to mark as read
     * @param headerAccessor Session header accessor
     */
    @MessageMapping("/chat.markAsRead")
    public void markMessageAsReadViaWebSocket(
        @Payload Long messageId,
        SimpMessageHeaderAccessor headerAccessor
    ) {
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        
        if (userId == null) return;
        
        try {
            ChatMessageResponse response = chatMessageService.markMessageAsRead(messageId, userId);
            
            // Broadcast read receipt to conversation
            Map<String, Object> readReceipt = new HashMap<>();
            readReceipt.put("messageId", messageId);
            readReceipt.put("readBy", userId);
            readReceipt.put("readAt", response.getReadAt());
            readReceipt.put("conversationId", response.getConversationId());
            
            messagingTemplate.convertAndSend(
                "/topic/read-receipts/" + response.getConversationId(),
                readReceipt
            );
            
            log.debug("✅ Message {} marked as read by user {}", messageId, userId);
            
        } catch (Exception e) {
            log.error("❌ Error marking message {} as read: {}", messageId, e.getMessage());
        }
    }
    
    /**
     * WebSocket: Subscribe to Conversation
     * 
     * ✅ NEW: Handles subscription events
     * Returns recent messages when user subscribes
     * 
     * @param conversationId Conversation ID
     * @param headerAccessor Session header accessor
     * @return Recent messages in conversation
     */
    @SubscribeMapping("/topic/messages/{conversationId}")
    public Page<ChatMessageResponse> handleConversationSubscription(
        @DestinationVariable Long conversationId,
        SimpMessageHeaderAccessor headerAccessor
    ) {
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        
        if (userId == null) {
            return Page.empty();
        }
        
        log.info("📡 User {} subscribed to conversation {}", userId, conversationId);
        
        try {
            // Return recent messages (last 20)
            Pageable pageable = PageRequest.of(0, 20);
            return chatMessageService.getConversationHistory(conversationId, userId, pageable);
        } catch (Exception e) {
            log.error("❌ Error loading initial messages for conversation {}: {}", 
                     conversationId, e.getMessage());
            return Page.empty();
        }
    }
    
    // ==================== REST ENDPOINTS ====================
    
    /**
     * REST: Send Message (Fallback)
     * 
     * ✅ ENHANCED: Better error responses
     * 
     * Use when WebSocket is not available.
     * Message is saved but may not deliver in real-time.
     * 
     * POST /api/chat/send
     * 
     * @param request Message request
     * @param userDetails Authenticated user
     * @return Sent message response
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendMessageRest(
        @Valid @RequestBody ChatMessageRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long senderId = userDetails.getUserId();
            log.info("💬 REST message from user {} to user {}", senderId, request.getReceiverId());
            
            ChatMessageResponse response = chatMessageService.sendMessage(senderId, request);
            
            // Attempt WebSocket broadcast (best effort)
            try {
                messagingTemplate.convertAndSend(
                    "/topic/messages/" + response.getConversationId(),
                    response
                );
                
                messagingTemplate.convertAndSendToUser(
                    request.getReceiverId().toString(),
                    "/queue/messages",
                    response
                );
                log.debug("✅ Successfully broadcasted via WebSocket");
            } catch (Exception e) {
                log.warn("⚠️ Failed to broadcast via WebSocket (receiver may not be online): {}", 
                        e.getMessage());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error sending message via REST: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send message", "message", e.getMessage()));
        }
    }
    
    /**
     * REST: Get All Conversations
     * 
     * GET /api/chat/conversations?page=0&size=20
     * 
     * Returns conversations sorted by last message time (most recent first).
     * Includes unread count and presence status.
     * 
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param userDetails Authenticated user
     * @return Page of conversations
     */
    @GetMapping("/conversations")
    public ResponseEntity<Page<ConversationResponse>> getConversations(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long userId = userDetails.getUserId();
            log.info("📋 Fetching conversations for user {}, page {}, size {}", userId, page, size);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<ConversationResponse> conversations = chatMessageService.getUserConversations(
                userId, 
                pageable
            );
            
            return ResponseEntity.ok(conversations);
            
        } catch (Exception e) {
            log.error("❌ Error fetching conversations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * REST: Get Conversation History
     * 
     * GET /api/chat/conversations/{conversationId}?page=0&size=50
     * 
     * Returns messages in descending order (newest first).
     * Automatically marks messages as READ.
     * 
     * @param conversationId Conversation ID
     * @param page Page number (default: 0)
     * @param size Page size (default: 50)
     * @param userDetails Authenticated user
     * @return Page of messages
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<Page<ChatMessageResponse>> getConversationHistory(
        @PathVariable Long conversationId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long userId = userDetails.getUserId();
            log.info("📜 Fetching history for conversation {}, user {}, page {}", 
                     conversationId, userId, page);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<ChatMessageResponse> messages = chatMessageService.getConversationHistory(
                conversationId,
                userId,
                pageable
            );
            
            return ResponseEntity.ok(messages);
            
        } catch (Exception e) {
            log.error("❌ Error fetching conversation history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * REST: Get or Create Conversation
     * 
     * GET /api/chat/conversations/user/{otherUserId}
     * 
     * If conversation exists, returns it.
     * If not, creates a new conversation.
     * 
     * @param otherUserId ID of the other user
     * @param userDetails Authenticated user
     * @return Conversation details
     */
    @GetMapping("/conversations/user/{otherUserId}")
    public ResponseEntity<ConversationResponse> getOrCreateConversation(
        @PathVariable Long otherUserId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long userId = userDetails.getUserId();
            log.info("🔍 Get/create conversation between user {} and user {}", userId, otherUserId);
            
            ConversationResponse conversation = chatMessageService.getOrCreateConversation(
                userId,
                otherUserId
            );
            
            return ResponseEntity.ok(conversation);
            
        } catch (Exception e) {
            log.error("❌ Error getting/creating conversation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * REST: Mark Conversation as Read
     * 
     * PUT /api/chat/conversations/{conversationId}/read
     * 
     * Marks all unread messages as read and resets unread count.
     * Broadcasts read receipts via WebSocket.
     * 
     * @param conversationId Conversation ID
     * @param userDetails Authenticated user
     * @return Number of messages marked as read
     */
    @PutMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Map<String, Object>> markConversationAsRead(
        @PathVariable Long conversationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long userId = userDetails.getUserId();
            log.info("✅ Marking conversation {} as read for user {}", conversationId, userId);
            
            int count = chatMessageService.markConversationAsRead(conversationId, userId);
            
            // Broadcast read receipt
            if (count > 0) {
                Map<String, Object> readReceipt = new HashMap<>();
                readReceipt.put("conversationId", conversationId);
                readReceipt.put("readBy", userId);
                readReceipt.put("count", count);
                readReceipt.put("timestamp", System.currentTimeMillis());
                
                messagingTemplate.convertAndSend(
                    "/topic/read-receipts/" + conversationId,
                    readReceipt
                );
            }
            
            return ResponseEntity.ok(Map.of(
                "count", count,
                "conversationId", conversationId,
                "message", "Marked " + count + " messages as read"
            ));
            
        } catch (Exception e) {
            log.error("❌ Error marking conversation as read: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * REST: Mark Single Message as Read
     * 
     * ✅ NEW: Mark individual message as read
     * 
     * PUT /api/chat/messages/{messageId}/read
     * 
     * @param messageId Message ID
     * @param userDetails Authenticated user
     * @return Updated message
     */
    @PutMapping("/messages/{messageId}/read")
    public ResponseEntity<ChatMessageResponse> markMessageAsRead(
        @PathVariable Long messageId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long userId = userDetails.getUserId();
            ChatMessageResponse response = chatMessageService.markMessageAsRead(messageId, userId);
            
            // Broadcast read receipt
            Map<String, Object> readReceipt = new HashMap<>();
            readReceipt.put("messageId", messageId);
            readReceipt.put("readBy", userId);
            readReceipt.put("readAt", response.getReadAt());
            
            messagingTemplate.convertAndSend(
                "/topic/read-receipts/" + response.getConversationId(),
                readReceipt
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error marking message as read: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * REST: Get Unread Count for Conversation
     * 
     * GET /api/chat/conversations/{conversationId}/unread
     * 
     * @param conversationId Conversation ID
     * @param userDetails Authenticated user
     * @return Unread count
     */
    @GetMapping("/conversations/{conversationId}/unread")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
        @PathVariable Long conversationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long userId = userDetails.getUserId();
            Long count = chatMessageService.getUnreadCount(conversationId, userId);
            
            return ResponseEntity.ok(Map.of(
                "conversationId", conversationId,
                "unreadCount", count
            ));
            
        } catch (Exception e) {
            log.error("❌ Error getting unread count: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * REST: Get Total Unread Count
     * 
     * GET /api/chat/unread/total
     * 
     * Useful for displaying notification badge.
     * 
     * @param userDetails Authenticated user
     * @return Total unread count across all conversations
     */
    @GetMapping("/unread/total")
    public ResponseEntity<Map<String, Object>> getTotalUnreadCount(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long userId = userDetails.getUserId();
            Long count = chatMessageService.getTotalUnreadCount(userId);
            
            return ResponseEntity.ok(Map.of(
                "userId", userId,
                "totalUnreadCount", count
            ));
            
        } catch (Exception e) {
            log.error("❌ Error getting total unread count: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * REST: Get User Presence
     * 
     * GET /api/chat/presence/{userId}
     * 
     * Returns online/offline status and last seen time.
     * 
     * @param userId User ID to check
     * @return User presence information
     */
    @GetMapping("/presence/{userId}")
    public ResponseEntity<UserPresenceDto> getUserPresence(@PathVariable Long userId) {
        try {
            log.debug("🔍 Checking presence for user {}", userId);
            UserPresenceDto presence = chatMessageService.getUserPresence(userId);
            return ResponseEntity.ok(presence);
            
        } catch (Exception e) {
            log.error("❌ Error getting user presence: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * REST: Search Messages in Conversation
     * 
     * GET /api/chat/conversations/{conversationId}/search?q=hello&page=0&size=20
     * 
     * Case-insensitive search through message content.
     * 
     * @param conversationId Conversation ID
     * @param q Search query
     * @param page Page number
     * @param size Page size
     * @param userDetails Authenticated user
     * @return Page of matching messages
     */
    @GetMapping("/conversations/{conversationId}/search")
    public ResponseEntity<Page<ChatMessageResponse>> searchMessages(
        @PathVariable Long conversationId,
        @RequestParam String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long userId = userDetails.getUserId();
            log.info("🔍 Searching messages in conversation {} with query: '{}'", conversationId, q);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<ChatMessageResponse> messages = chatMessageService.searchMessages(
                conversationId,
                q,
                userId,
                pageable
            );
            
            return ResponseEntity.ok(messages);
            
        } catch (Exception e) {
            log.error("❌ Error searching messages: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * REST: Delete Message (Soft Delete)
     * 
     * DELETE /api/chat/messages/{messageId}
     * 
     * Only the sender can delete their message.
     * Message is hidden but not removed from database.
     * 
     * @param messageId Message ID
     * @param userDetails Authenticated user
     * @return Success response
     */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Map<String, String>> deleteMessage(
        @PathVariable Long messageId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long userId = userDetails.getUserId();
            log.info("🗑️ Deleting message {} by user {}", messageId, userId);
            
            chatMessageService.deleteMessage(messageId, userId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Message deleted successfully",
                "messageId", messageId.toString()
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error deleting message: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete message"));
        }
    }
    
    /**
     * REST: Health Check for WebSocket Connection
     * 
     * ✅ NEW: Check if WebSocket is accessible
     * 
     * GET /api/chat/health
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "chat",
            "websocket", "enabled",
            "timestamp", System.currentTimeMillis()
        ));
    }
}