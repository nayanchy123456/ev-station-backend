package com.evstation.ev_charging_backend.controller;

import com.evstation.ev_charging_backend.dto.*;
import com.evstation.ev_charging_backend.enums.ConversationType;
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
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * COMPLETE ENHANCED Chat Controller for Real-Time Messaging
 * 
 * Features:
 * - Role-based conversations (USER-HOST, USER-ADMIN, HOST-ADMIN)
 * - Real-time WebSocket messaging
 * - Admin search and chat functionality
 * - Charger-specific conversations
 * - Typing indicators
 * - Read receipts
 * - Conversation management
 * 
 * REST Endpoints:
 * - POST   /api/chat/conversations/initiate       → Initiate conversation with context
 * - GET    /api/chat/conversations                → List all conversations
 * - GET    /api/chat/conversations/type/{type}    → Filter by conversation type
 * - GET    /api/chat/conversations/{id}           → Get conversation details
 * - GET    /api/chat/conversations/{id}/messages  → Get conversation messages
 * - PUT    /api/chat/conversations/{id}/read      → Mark conversation as read
 * - PUT    /api/chat/conversations/{id}/archive   → Archive/unarchive conversation
 * - GET    /api/chat/conversations/search         → Search conversations
 * 
 * Admin Endpoints:
 * - GET    /api/chat/admin/support                → Get support conversations
 * - POST   /api/chat/admin/search-users           → Search users for chat
 * - POST   /api/chat/admin/initiate               → Initiate chat with user
 * 
 * Charger Endpoints:
 * - GET    /api/chat/charger/{id}/host            → Get charger host for chat
 * - GET    /api/chat/charger/{id}/conversations   → Get conversations about charger
 * 
 * WebSocket Endpoints:
 * - /app/chat.sendMessage                         → Send message
 * - /app/chat.typing                              → Typing indicator
 * - /app/chat.stopTyping                          → Stop typing
 * - /topic/messages/{conversationId}              → Subscribe to conversation
 * - /user/queue/messages                          → Personal message queue
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://localhost:5174"})
public class ChatController {
    
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    
    // ==================== WEBSOCKET ENDPOINTS ====================
    
    /**
     * WebSocket: Send Message
     * 
     * Client sends to: /app/chat.sendMessage
     * Server broadcasts to: 
     *   1. /topic/messages/{conversationId}
     *   2. /user/{receiverId}/queue/messages
     * 
     * @param request Message request
     * @param headerAccessor Session header accessor
     * @return Message response
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
            log.error("❌ Unauthenticated WebSocket message attempt");
            throw new IllegalStateException("User not authenticated");
        }
        
        log.info("💬 WebSocket message from {} ({}) to {}", senderUsername, senderId, request.getReceiverId());
        
        try {
            // Send message through service
            ChatMessageResponse response = chatMessageService.sendMessage(senderId, request);
            
            // Broadcast to conversation topic
            messagingTemplate.convertAndSend(
                "/topic/messages/" + response.getConversationId(),
                response
            );
            
            // Send to receiver's personal queue
            messagingTemplate.convertAndSendToUser(
                request.getReceiverId().toString(),
                "/queue/messages",
                response
            );
            
            // Send confirmation to sender
            Map<String, Object> confirmation = new HashMap<>();
            confirmation.put("messageId", response.getId());
            confirmation.put("status", "sent");
            confirmation.put("timestamp", response.getCreatedAt());
            
            messagingTemplate.convertAndSendToUser(
                senderId.toString(),
                "/queue/confirmations",
                confirmation
            );
            
            log.info("✅ Message {} sent successfully", response.getId());
            
            return response;
            
        } catch (Exception e) {
            log.error("❌ Error sending message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send message: " + e.getMessage());
        }
    }
    
    /**
     * WebSocket: Typing Indicator
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
    }
    
    /**
     * WebSocket: Stop Typing Indicator
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
    }
    
    // ==================== CONVERSATION MANAGEMENT ====================
    
    /**
     * REST: Initiate or Get Conversation with Context
     * 
     * POST /api/chat/conversations/initiate
     * 
     * This endpoint handles:
     * - User initiating chat with host about a charger
     * - User/Host requesting support from admin
     * - Admin initiating chat with user/host
     * 
     * @param request Conversation initiation request
     * @param userDetails Authenticated user
     * @return Conversation response
     */
    @PostMapping("/conversations/initiate")
    public ResponseEntity<ConversationResponse> initiateConversation(
        @RequestBody @Valid ConversationInitiateRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long currentUserId = userDetails.getUserId();
            log.info("🆕 User {} initiating conversation with user {}, type: {}", 
                     currentUserId, request.getParticipantId(), request.getConversationType());
            
            ConversationResponse conversation = chatMessageService
                .initiateConversation(currentUserId, request);
            
            return ResponseEntity.ok(conversation);
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ Error initiating conversation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * REST: Get All Conversations for Current User
     * 
     * GET /api/chat/conversations?page=0&size=20
     * 
     * @param page Page number
     * @param size Page size
     * @param userDetails Authenticated user
     * @return Page of conversations
     */
    @GetMapping("/conversations")
    public ResponseEntity<Page<ConversationResponse>> getUserConversations(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long userId = userDetails.getUserId();
            log.info("📋 Fetching conversations for user {} (page {}, size {})", userId, page, size);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<ConversationResponse> conversations = chatMessageService
                .getUserConversations(userId, pageable);
            
            return ResponseEntity.ok(conversations);
            
        } catch (Exception e) {
            log.error("❌ Error fetching conversations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * REST: Get Conversations Filtered by Type
     * 
     * GET /api/chat/conversations/type/USER_HOST?page=0&size=20
     * 
     * @param type Conversation type
     * @param page Page number
     * @param size Page size
     * @param userDetails Authenticated user
     * @return Page of conversations
     */
    @GetMapping("/conversations/type/{type}")
    public ResponseEntity<Page<ConversationResponse>> getConversationsByType(
        @PathVariable ConversationType type,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long userId = userDetails.getUserId();
            log.info("📋 Fetching {} conversations for user {}", type, userId);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<ConversationResponse> conversations = chatMessageService
                .getUserConversationsByType(userId, type, pageable);
            
            return ResponseEntity.ok(conversations);
            
        } catch (Exception e) {
            log.error("❌ Error fetching conversations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // CONTINUED IN NEXT FILE...// CONTINUATION OF ChatController.java
    
    /**
     * REST: Get Specific Conversation
     * 
     * GET /api/chat/conversations/{conversationId}
     * 
     * @param conversationId Conversation ID
     * @param userDetails Authenticated user
     * @return Conversation details
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ConversationResponse> getConversation(
        @PathVariable Long conversationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long userId = userDetails.getUserId();
            log.info("🔍 Fetching conversation {} for user {}", conversationId, userId);
            
            ConversationResponse conversation = chatMessageService
                .getConversationById(conversationId, userId);
            
            return ResponseEntity.ok(conversation);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("❌ Error fetching conversation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * REST: Get Messages in Conversation
     * 
     * GET /api/chat/conversations/{conversationId}/messages?page=0&size=50
     * 
     * @param conversationId Conversation ID
     * @param page Page number
     * @param size Page size
     * @param userDetails Authenticated user
     * @return Page of messages
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Page<ChatMessageResponse>> getConversationMessages(
        @PathVariable Long conversationId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long userId = userDetails.getUserId();
            log.info("📨 Fetching messages for conversation {} by user {}", conversationId, userId);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<ChatMessageResponse> messages = chatMessageService
                .getConversationMessages(conversationId, userId, pageable);
            
            return ResponseEntity.ok(messages);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("❌ Error fetching messages: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * REST: Mark Conversation as Read
     * 
     * PUT /api/chat/conversations/{conversationId}/read
     * 
     * @param conversationId Conversation ID
     * @param userDetails Authenticated user
     * @return Success response
     */
    @PutMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Map<String, Object>> markConversationAsRead(
        @PathVariable Long conversationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long userId = userDetails.getUserId();
            log.info("👁️ Marking conversation {} as read by user {}", conversationId, userId);
            
            Integer count = chatMessageService.markConversationAsRead(conversationId, userId);
            
            // Broadcast read receipt
            Map<String, Object> readReceipt = new HashMap<>();
            readReceipt.put("conversationId", conversationId);
            readReceipt.put("readBy", userId);
            readReceipt.put("count", count);
            
            messagingTemplate.convertAndSend(
                "/topic/read-receipts/" + conversationId,
                readReceipt
            );
            
            return ResponseEntity.ok(Map.of(
                "conversationId", conversationId,
                "markedAsRead", count
            ));
            
        } catch (Exception e) {
            log.error("❌ Error marking conversation as read: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * REST: Archive/Unarchive Conversation
     * 
     * PUT /api/chat/conversations/{conversationId}/archive?archive=true
     * 
     * @param conversationId Conversation ID
     * @param archive true to archive, false to unarchive
     * @param userDetails Authenticated user
     * @return Success response
     */
    @PutMapping("/conversations/{conversationId}/archive")
    public ResponseEntity<Map<String, String>> toggleArchiveConversation(
        @PathVariable Long conversationId,
        @RequestParam boolean archive,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long userId = userDetails.getUserId();
            log.info("📦 {} conversation {} for user {}", 
                     archive ? "Archiving" : "Unarchiving", conversationId, userId);
            
            chatMessageService.toggleArchiveConversation(conversationId, userId, archive);
            
            return ResponseEntity.ok(Map.of(
                "message", archive ? "Conversation archived" : "Conversation unarchived",
                "conversationId", conversationId.toString()
            ));
            
        } catch (Exception e) {
            log.error("❌ Error archiving conversation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * REST: Search Conversations
     * 
     * GET /api/chat/conversations/search?q=hello&page=0&size=20
     * 
     * @param searchTerm Search term
     * @param page Page number
     * @param size Page size
     * @param userDetails Authenticated user
     * @return Page of matching conversations
     */
    @GetMapping("/conversations/search")
    public ResponseEntity<Page<ConversationResponse>> searchConversations(
        @RequestParam String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long userId = userDetails.getUserId();
            log.info("🔍 Searching conversations for user {} with query: '{}'", userId, q);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<ConversationResponse> conversations = chatMessageService
                .searchConversations(userId, q, pageable);
            
            return ResponseEntity.ok(conversations);
            
        } catch (Exception e) {
            log.error("❌ Error searching conversations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ==================== ADMIN ENDPOINTS ====================
    
    /**
     * REST: Get Admin Support Conversations
     * 
     * GET /api/chat/admin/support?page=0&size=20
     * 
     * Admin endpoint to view all USER_ADMIN and HOST_ADMIN conversations
     * 
     * @param page Page number
     * @param size Page size
     * @param userDetails Authenticated user (must be admin)
     * @return Page of support conversations
     */
    @GetMapping("/admin/support")
    public ResponseEntity<Page<ConversationResponse>> getAdminSupportConversations(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long adminId = userDetails.getUserId();
            log.info("👨‍💼 Admin {} fetching support conversations", adminId);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<ConversationResponse> conversations = chatMessageService
                .getAdminSupportConversations(adminId, pageable);
            
            return ResponseEntity.ok(conversations);
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Unauthorized admin access: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("❌ Error fetching admin support conversations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * REST: Search Users for Admin Chat
     * 
     * POST /api/chat/admin/search-users
     * 
     * Admin endpoint to search for users/hosts to initiate chat
     * 
     * @param request Search request with filters
     * @param userDetails Authenticated user (must be admin)
     * @return Page of users matching search criteria
     */
    @PostMapping("/admin/search-users")
    public ResponseEntity<Page<UserSearchResponse>> searchUsersForAdminChat(
        @RequestBody AdminChatSearchRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long adminId = userDetails.getUserId();
            log.info("👨‍💼 Admin {} searching users with term: '{}'", adminId, request.getSearchTerm());
            
            Page<UserSearchResponse> users = chatMessageService
                .searchUsersForAdminChat(adminId, request);
            
            return ResponseEntity.ok(users);
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Unauthorized admin access: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("❌ Error searching users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * REST: Admin Initiate Chat
     * 
     * POST /api/chat/admin/initiate
     * 
     * Admin initiates a support conversation with a user or host
     * 
     * @param request Contains target user ID and optional initial message
     * @param userDetails Authenticated user (must be admin)
     * @return Created conversation
     */
    @PostMapping("/admin/initiate")
    public ResponseEntity<ConversationResponse> adminInitiateChat(
        @RequestBody Map<String, Object> request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long adminId = userDetails.getUserId();
            Long targetUserId = Long.valueOf(request.get("targetUserId").toString());
            String initialMessage = request.get("initialMessage") != null 
                ? request.get("initialMessage").toString() 
                : null;
            
            log.info("👨‍💼 Admin {} initiating chat with user {}", adminId, targetUserId);
            
            ConversationResponse conversation = chatMessageService
                .adminInitiateChat(adminId, targetUserId, initialMessage);
            
            return ResponseEntity.ok(conversation);
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid admin request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ Error admin initiating chat: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ==================== CHARGER ENDPOINTS ====================
    
    /**
     * REST: Get Charger Host ID
     * 
     * GET /api/chat/charger/{chargerId}/host
     * 
     * Get the host user ID for a specific charger
     * Useful for initiating USER_HOST conversation
     * 
     * @param chargerId Charger ID
     * @return Map with host ID
     */
    @GetMapping("/charger/{chargerId}/host")
    public ResponseEntity<Map<String, Long>> getChargerHost(@PathVariable Long chargerId) {
        try {
            log.info("🔌 Fetching host for charger {}", chargerId);
            
            Long hostId = chatMessageService.getChargerHostId(chargerId);
            
            return ResponseEntity.ok(Map.of("hostId", hostId));
            
        } catch (Exception e) {
            log.error("❌ Error fetching charger host: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * REST: Get Conversations About Charger
     * 
     * GET /api/chat/charger/{chargerId}/conversations?page=0&size=20
     * 
     * Get all conversations related to a specific charger (for host)
     * 
     * @param chargerId Charger ID
     * @param page Page number
     * @param size Page size
     * @param userDetails Authenticated user (must be host)
     * @return Page of conversations
     */
    @GetMapping("/charger/{chargerId}/conversations")
    public ResponseEntity<Page<ConversationResponse>> getChargerConversations(
        @PathVariable Long chargerId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long hostId = userDetails.getUserId();
            log.info("🔌 Fetching conversations for charger {} by host {}", chargerId, hostId);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<ConversationResponse> conversations = chatMessageService
                .getChargerConversations(chargerId, hostId, pageable);
            
            return ResponseEntity.ok(conversations);
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Unauthorized access: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("❌ Error fetching charger conversations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ==================== UTILITY ENDPOINTS ====================
    
    /**
     * REST: Get Total Unread Count
     * 
     * GET /api/chat/unread/total
     * 
     * @param userDetails Authenticated user
     * @return Total unread count
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
     * @param userId User ID
     * @return User presence information
     */
    @GetMapping("/presence/{userId}")
    public ResponseEntity<UserPresenceDto> getUserPresence(@PathVariable Long userId) {
        try {
            UserPresenceDto presence = chatMessageService.getUserPresence(userId);
            return ResponseEntity.ok(presence);
            
        } catch (Exception e) {
            log.error("❌ Error getting user presence: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * REST: Health Check
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