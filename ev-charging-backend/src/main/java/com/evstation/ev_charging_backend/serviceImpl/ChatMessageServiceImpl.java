package com.evstation.ev_charging_backend.serviceImpl;

import com.evstation.ev_charging_backend.dto.*;
import com.evstation.ev_charging_backend.entity.*;
import com.evstation.ev_charging_backend.enums.*;
import com.evstation.ev_charging_backend.exception.ResourceNotFoundException;
import com.evstation.ev_charging_backend.repository.*;
import com.evstation.ev_charging_backend.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Complete Enhanced ChatMessageService Implementation
 * 
 * Features:
 * - Support for different conversation types
 * - User-Host chat through chargers
 * - User/Host support chat with admin
 * - Admin can search and chat with anyone
 * - Real-time presence tracking
 * - Message read receipts
 * - Conversation archiving
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageServiceImpl implements ChatMessageService {
    
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final UserPresenceRepository userPresenceRepository;
    private final UserRepository userRepository;
    private final ChargerRepository chargerRepository;
    
    private static final int MAX_MESSAGE_LENGTH = 5000;
    private static final int MESSAGE_PREVIEW_LENGTH = 100;
    
    // ==================== MESSAGE OPERATIONS ====================
    
    @Override
    @Transactional
    public ChatMessageResponse sendMessage(Long senderId, ChatMessageRequest request) {
        log.info("📤 Sending message from user {} to user {}", senderId, request.getReceiverId());
        
        // Validate input
        validateMessageRequest(senderId, request);
        
        // Load users
        User sender = loadUser(senderId, "Sender");
        User receiver = loadUser(request.getReceiverId(), "Receiver");
        
        // Prevent self-messaging
        if (senderId.equals(request.getReceiverId())) {
            throw new IllegalArgumentException("Cannot send message to yourself");
        }
        
        // Find or create conversation
        // For backward compatibility, if no type specified, create DIRECT conversation
        ConversationType type = request.getConversationType() != null 
            ? request.getConversationType() 
            : ConversationType.DIRECT;
        
        Conversation conversation = findOrCreateConversation(
            senderId, 
            request.getReceiverId(), 
            type,
            request.getChargerId()
        );
        
        // Create message entity
        ChatMessage message = ChatMessage.builder()
            .conversation(conversation)
            .sender(sender)
            .receiver(receiver)
            .content(request.getContent().trim())
            .status(MessageStatus.SENT)
            .isDeleted(false)
            .build();
        
        // Save message
        ChatMessage savedMessage = chatMessageRepository.save(message);
        log.debug("💾 Message saved with ID: {}", savedMessage.getId());
        
        // Update conversation metadata
        updateConversationMetadata(
            conversation, 
            request.getContent(), 
            savedMessage.getCreatedAt(), 
            request.getReceiverId(),
            senderId
        );
        
        // Check if receiver is online and mark as delivered
        boolean isReceiverOnline = isUserOnline(request.getReceiverId());
        if (isReceiverOnline) {
            savedMessage.markAsDelivered();
            chatMessageRepository.save(savedMessage);
            log.debug("✅ Message marked as delivered (receiver online)");
        }
        
        log.info("✅ Message sent successfully: ID={}", savedMessage.getId());
        
        return mapToMessageResponse(savedMessage, senderId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getConversationMessages(
        Long conversationId, 
        Long userId, 
        Pageable pageable
    ) {
        log.info("📨 Fetching messages for conversation {} by user {}", conversationId, userId);
        
        // Verify conversation exists and user is participant
        Conversation conversation = getConversationOrThrow(conversationId);
        validateUserIsParticipant(conversation, userId);
        
        // Fetch messages
        Page<ChatMessage> messages = chatMessageRepository
            .findByConversationIdAndIsDeletedFalse(conversationId, pageable);
        
        log.info("Found {} messages in conversation {}", messages.getTotalElements(), conversationId);
        
        return messages.map(msg -> mapToMessageResponse(msg, userId));
    }
    
    @Override
    @Transactional
    public ChatMessageResponse markMessageAsRead(Long messageId, Long userId) {
        log.info("👁️ Marking message {} as read by user {}", messageId, userId);
        
        ChatMessage message = chatMessageRepository.findById(messageId)
            .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        
        // Only receiver can mark message as read
        if (!message.getReceiver().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Only receiver can mark message as read");
        }
        
        // Mark as read if not already
        if (!message.isRead()) {
            message.markAsRead();
            chatMessageRepository.save(message);
            log.debug("✅ Message marked as read");
        }
        
        return mapToMessageResponse(message, userId);
    }
    
    @Override
    @Transactional
    public Integer markConversationAsRead(Long conversationId, Long userId) {
        log.info("👁️ Marking all messages in conversation {} as read by user {}", 
                 conversationId, userId);
        
        Conversation conversation = getConversationOrThrow(conversationId);
        validateUserIsParticipant(conversation, userId);
        
        // Get all unread messages where user is receiver
        List<ChatMessage> unreadMessages = chatMessageRepository
            .findUnreadMessagesForUser(conversationId, userId);
        
        // Mark all as read
        unreadMessages.forEach(ChatMessage::markAsRead);
        chatMessageRepository.saveAll(unreadMessages);
        
        // Reset unread count in conversation
        conversation.resetUnreadCount(userId);
        conversationRepository.save(conversation);
        
        log.info("✅ Marked {} messages as read in conversation {}", 
                 unreadMessages.size(), conversationId);
        
        return unreadMessages.size();
    }
    
    @Override
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        log.info("🗑️ Deleting message {} by user {}", messageId, userId);
        
        ChatMessage message = chatMessageRepository.findById(messageId)
            .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        
        // Only sender can delete message
        if (!message.getSender().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Only sender can delete their own message");
        }
        
        // Soft delete
        message.softDelete();
        chatMessageRepository.save(message);
        
        log.info("✅ Message soft deleted: {}", messageId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> searchMessages(
        Long conversationId,
        String searchTerm,
        Long userId,
        Pageable pageable
    ) {
        log.info("🔍 Searching messages in conversation {} with term: '{}'", 
                 conversationId, searchTerm);
        
        Conversation conversation = getConversationOrThrow(conversationId);
        validateUserIsParticipant(conversation, userId);
        
        Page<ChatMessage> messages = chatMessageRepository
            .searchInConversation(conversationId, searchTerm, pageable);
        
        log.info("Found {} matching messages", messages.getTotalElements());
        
        return messages.map(msg -> mapToMessageResponse(msg, userId));
    }
    
    // ==================== CONVERSATION OPERATIONS ====================
    
   @Override
@Transactional
public ConversationResponse initiateConversation(
    Long currentUserId,
    ConversationInitiateRequest request
) {
    log.info("🆕 Initiating conversation: user {} → user {}, type: {}", 
             currentUserId, request.getParticipantId(), request.getConversationType());
    
    // Validate request
    if (request.getParticipantId() == null) {
        throw new IllegalArgumentException("Participant ID is required");
    }
    
    if (currentUserId.equals(request.getParticipantId())) {
        throw new IllegalArgumentException("Cannot create conversation with yourself");
    }
    
    // Verify participant exists
    User participant = loadUser(request.getParticipantId(), "Participant");
    
    // Determine conversation type
    ConversationType type = request.getConversationType() != null 
        ? request.getConversationType() 
        : ConversationType.DIRECT;
    
    // For USER_HOST type, verify charger exists
    Long chargerId = request.getChargerId();
    if (type == ConversationType.USER_HOST) {
        if (chargerId == null) {
            throw new IllegalArgumentException("Charger ID is required for USER_HOST conversation");
        }
        
        // Verify charger exists and participant is the host
        Charger charger = chargerRepository.findById(chargerId)
            .orElseThrow(() -> new ResourceNotFoundException("Charger not found"));
        
        if (!charger.getHost().getUserId().equals(request.getParticipantId())) {
            throw new IllegalArgumentException(
                "Participant is not the host of the specified charger"
            );
        }
    }
    
    // ✅ NEW: Check if conversation already exists
    Conversation existingConv = conversationRepository
        .findByParticipantsAndTypeAndCharger(
            currentUserId, 
            request.getParticipantId(), 
            type, 
            chargerId
        )
        .orElse(null);
    
    boolean conversationAlreadyExists = (existingConv != null);
    
    // Get or create conversation
    Conversation conversation = conversationAlreadyExists 
        ? existingConv 
        : findOrCreateConversation(currentUserId, request.getParticipantId(), type, chargerId);
    
    // Set title if provided
    if (request.getTitle() != null && !request.getTitle().isBlank()) {
        conversation.setTitle(request.getTitle());
        conversationRepository.save(conversation);
    }
    
    // ✅ FIXED: Only send initial message if conversation is NEW
    if (!conversationAlreadyExists && 
        request.getInitialMessage() != null && 
        !request.getInitialMessage().isBlank()) {
        
        log.info("📤 Sending initial message to new conversation");
        
        ChatMessageRequest messageRequest = ChatMessageRequest.builder()
            .receiverId(request.getParticipantId())
            .content(request.getInitialMessage())
            .conversationType(type)
            .chargerId(chargerId)
            .build();
        
        sendMessage(currentUserId, messageRequest);
    } else if (conversationAlreadyExists) {
        log.info("♻️ Conversation already exists, skipping initial message");
    }
    
    return mapToConversationResponse(conversation, currentUserId);
}
    
    @Override
    @Transactional
    public ConversationResponse getOrCreateConversation(
        Long user1Id,
        Long user2Id,
        ConversationType type,
        Long chargerId
    ) {
        Conversation conversation = findOrCreateConversation(user1Id, user2Id, type, chargerId);
        return mapToConversationResponse(conversation, user1Id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> getUserConversations(Long userId, Pageable pageable) {
        log.info("📋 Fetching conversations for user {}", userId);
        
        Page<Conversation> conversations = conversationRepository.findByUserId(userId, pageable);
        
        log.info("Found {} conversations for user {}", conversations.getTotalElements(), userId);
        
        return conversations.map(conv -> mapToConversationResponse(conv, userId));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> getUserConversationsByType(
        Long userId,
        ConversationType type,
        Pageable pageable
    ) {
        log.info("📋 Fetching {} conversations for user {}", type, userId);
        
        Page<Conversation> conversations = conversationRepository
            .findByUserIdAndType(userId, type, pageable);
        
        log.info("Found {} {} conversations", conversations.getTotalElements(), type);
        
        return conversations.map(conv -> mapToConversationResponse(conv, userId));
    }
    
    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversationById(Long conversationId, Long userId) {
        log.info("🔍 Fetching conversation {} for user {}", conversationId, userId);
        
        Conversation conversation = getConversationOrThrow(conversationId);
        validateUserIsParticipant(conversation, userId);
        
        return mapToConversationResponse(conversation, userId);
    }
    
    @Override
    @Transactional
    public void toggleArchiveConversation(Long conversationId, Long userId, boolean archive) {
        log.info("📦 {} conversation {} for user {}", 
                 archive ? "Archiving" : "Unarchiving", conversationId, userId);
        
        Conversation conversation = getConversationOrThrow(conversationId);
        validateUserIsParticipant(conversation, userId);
        
        if (archive) {
            conversation.archiveForUser(userId);
        } else {
            conversation.unarchiveForUser(userId);
        }
        
        conversationRepository.save(conversation);
        
        log.info("✅ Conversation {} {}", conversationId, archive ? "archived" : "unarchived");
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> searchConversations(
        Long userId,
        String searchTerm,
        Pageable pageable
    ) {
        log.info("🔍 Searching conversations for user {} with term: '{}'", userId, searchTerm);
        
        Page<Conversation> conversations = conversationRepository
            .searchConversations(userId, searchTerm, pageable);
        
        log.info("Found {} matching conversations", conversations.getTotalElements());
        
        return conversations.map(conv -> mapToConversationResponse(conv, userId));
    }
    
    // CONTINUED IN NEXT FILE...// CONTINUATION OF ChatMessageServiceImpl.java
    
    // ==================== ADMIN OPERATIONS ====================
    
    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> getAdminSupportConversations(
        Long adminId,
        Pageable pageable
    ) {
        log.info("👨‍💼 Fetching support conversations for admin {}", adminId);
        
        // Verify user is admin
        User admin = loadUser(adminId, "Admin");
        if (admin.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("User is not an admin");
        }
        
        Page<Conversation> conversations = conversationRepository
            .findAdminSupportConversations(adminId, pageable);
        
        log.info("Found {} support conversations", conversations.getTotalElements());
        
        return conversations.map(conv -> mapToConversationResponse(conv, adminId));
    }
    
 // ============================================
// REPLACE THIS METHOD IN ChatMessageServiceImpl.java
// Location: Around line 422-475
// ============================================

@Override
@Transactional(readOnly = true)
public Page<UserSearchResponse> searchUsersForAdminChat(
    Long userId,  // ⚠️ CHANGED FROM: adminId
    AdminChatSearchRequest request
) {
    log.info("🔍 User {} searching users with term: '{}', roleFilter: {}", 
        userId, request.getSearchTerm(), request.getRoleFilter());
    
    // Load the requesting user
    User requestingUser = loadUser(userId, "User");
    
    // Build search query
    String searchTerm = request.getSearchTerm() != null 
        ? request.getSearchTerm().trim() 
        : "";
    
    Page<User> users;
    
    // Check user permissions
    if (requestingUser.getRole() == Role.ADMIN) {
        // ✅ ADMINS can search for anyone (existing functionality)
        log.info("👨‍💼 Admin searching for users");
        
        if (searchTerm.isEmpty()) {
            if (request.getRoleFilter() != null) {
                users = userRepository.findByRoleAndUserIdNot(
                    request.getRoleFilter(),
                    userId,
                    Pageable.ofSize(request.getSize()).withPage(request.getPage())
                );
            } else {
                users = userRepository.findByUserIdNot(
                    userId,
                    Pageable.ofSize(request.getSize()).withPage(request.getPage())
                );
            }
        } else {
            if (request.getRoleFilter() != null) {
                users = userRepository.searchByTermAndRole(
                    searchTerm,
                    request.getRoleFilter(),
                    userId,
                    Pageable.ofSize(request.getSize()).withPage(request.getPage())
                );
            } else {
                users = userRepository.searchByTerm(
                    searchTerm,
                    userId,
                    Pageable.ofSize(request.getSize()).withPage(request.getPage())
                );
            }
        }
        
    } else if (request.getRoleFilter() == Role.ADMIN) {
        // ✅ USERS/HOSTS can ONLY search for ADMINS (for support) - THIS IS NEW!
        log.info("👤 User/Host {} searching for admins for support", userId);
        
        if (searchTerm.isEmpty()) {
            users = userRepository.findByRoleAndUserIdNot(
                Role.ADMIN,
                userId,
                Pageable.ofSize(request.getSize()).withPage(request.getPage())
            );
        } else {
            users = userRepository.searchByTermAndRole(
                searchTerm,
                Role.ADMIN,
                userId,
                Pageable.ofSize(request.getSize()).withPage(request.getPage())
            );
        }
        
    } else {
        // ❌ NON-ADMINS cannot search for other non-admins
        log.warn("❌ Unauthorized search attempt by user {} (role: {})", 
            userId, requestingUser.getRole());
        throw new IllegalArgumentException(
            "Only admins can search for users. Regular users can only search for admins for support."
        );
    }
    
    // Map to response with conversation info (existing code - unchanged)
    return users.map(user -> mapToUserSearchResponse(user, userId));
}

// ============================================
// THAT'S IT! No other changes needed in backend
// ============================================
    
    @Override
    @Transactional
    public ConversationResponse adminInitiateChat(
        Long adminId,
        Long targetUserId,
        String initialMessage
    ) {
        log.info("👨‍💼 Admin {} initiating chat with user {}", adminId, targetUserId);
        
        // Verify admin
        User admin = loadUser(adminId, "Admin");
        if (admin.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("User is not an admin");
        }
        
        // Verify target user exists
        User targetUser = loadUser(targetUserId, "Target User");
        
        // Determine conversation type based on target user role
        ConversationType type;
        if (targetUser.getRole() == Role.USER) {
            type = ConversationType.USER_ADMIN;
        } else if (targetUser.getRole() == Role.HOST || targetUser.getRole() == Role.PENDING_HOST) {
            type = ConversationType.HOST_ADMIN;
        } else {
            throw new IllegalArgumentException("Cannot create support conversation with admin");
        }
        
        // Get or create conversation
        Conversation conversation = findOrCreateConversation(
            targetUserId,  // User/Host ID
            adminId,       // Admin ID
            type,
            null  // No charger for support conversations
        );
        
        // Set default title if not set
        if (conversation.getTitle() == null || conversation.getTitle().isBlank()) {
            String title = type == ConversationType.USER_ADMIN 
                ? "Support Chat" 
                : "Host Support";
            conversation.setTitle(title);
            conversationRepository.save(conversation);
        }
        
        // Send initial message if provided
        if (initialMessage != null && !initialMessage.isBlank()) {
            ChatMessageRequest messageRequest = ChatMessageRequest.builder()
                .receiverId(targetUserId)
                .content(initialMessage)
                .conversationType(type)
                .build();
            
            sendMessage(adminId, messageRequest);
        }
        
        return mapToConversationResponse(conversation, adminId);
    }
    
    // ==================== CHARGER-SPECIFIC OPERATIONS ====================
    
    @Override
    @Transactional(readOnly = true)
    public Long getChargerHostId(Long chargerId) {
        Charger charger = chargerRepository.findById(chargerId)
            .orElseThrow(() -> new ResourceNotFoundException("Charger not found"));
        
        if (charger.getHost() == null) {
            throw new IllegalStateException("Charger has no host assigned");
        }
        
        return charger.getHost().getUserId();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> getChargerConversations(
        Long chargerId,
        Long hostId,
        Pageable pageable
    ) {
        log.info("🔌 Fetching conversations for charger {} by host {}", chargerId, hostId);
        
        // Verify charger exists and user is the host
        Charger charger = chargerRepository.findById(chargerId)
            .orElseThrow(() -> new ResourceNotFoundException("Charger not found"));
        
        if (!charger.getHost().getUserId().equals(hostId)) {
            throw new IllegalArgumentException("User is not the host of this charger");
        }
        
        Page<Conversation> conversations = conversationRepository
            .findByChargerId(chargerId, pageable);
        
        log.info("Found {} conversations for charger {}", conversations.getTotalElements(), chargerId);
        
        return conversations.map(conv -> mapToConversationResponse(conv, hostId));
    }
    
    // ==================== PRESENCE & STATUS ====================
    
    @Override
    @Transactional(readOnly = true)
    public UserPresenceDto getUserPresence(Long userId) {
        User user = loadUser(userId, "User");
        
        UserPresence presence = userPresenceRepository.findByUserId(userId)
            .orElse(null);
        
        return UserPresenceDto.builder()
            .userId(userId)
            .status(presence != null ? presence.getStatus() : UserPresenceStatus.OFFLINE)
            .isOnline(presence != null && presence.getStatus() == UserPresenceStatus.ONLINE)
            .lastSeenAt(presence != null ? presence.getLastSeenAt() : null)
            .build();
    }
    
    @Override
    @Transactional
    public void updateUserPresence(Long userId, boolean isOnline) {
        UserPresence presence = userPresenceRepository.findByUserId(userId)
            .orElse(UserPresence.builder()
                .userId(userId)
                .status(UserPresenceStatus.OFFLINE)
                .build());
        
        if (isOnline) {
            presence.setStatus(UserPresenceStatus.ONLINE);
        } else {
            presence.setStatus(UserPresenceStatus.OFFLINE);
            presence.setLastSeenAt(LocalDateTime.now());
        }
        
        userPresenceRepository.save(presence);
        
        log.debug("✅ User {} presence updated: {}", userId, isOnline ? "ONLINE" : "OFFLINE");
    }
    
    // ==================== UNREAD COUNT ====================
    
    @Override
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long conversationId, Long userId) {
        Conversation conversation = getConversationOrThrow(conversationId);
        validateUserIsParticipant(conversation, userId);
        
        return chatMessageRepository.countUnreadMessages(conversationId, userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Long getTotalUnreadCount(Long userId) {
        return conversationRepository.getTotalUnreadCount(userId);
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Find existing conversation or create new one
     */
    private Conversation findOrCreateConversation(
        Long user1Id,
        Long user2Id,
        ConversationType type,
        Long chargerId
    ) {
        // Try to find existing conversation
        return conversationRepository
            .findByParticipantsAndTypeAndCharger(user1Id, user2Id, type, chargerId)
            .orElseGet(() -> createNewConversation(user1Id, user2Id, type, chargerId));
    }
    
    /**
     * Create a new conversation
     */
    private Conversation createNewConversation(
        Long user1Id,
        Long user2Id,
        ConversationType type,
        Long chargerId
    ) {
        log.info("🆕 Creating new {} conversation between users {} and {}", 
                 type, user1Id, user2Id);
        
        // For DIRECT conversations, normalize user IDs (smaller first)
        // For typed conversations, keep as-is
        Long normalizedUser1Id = user1Id;
        Long normalizedUser2Id = user2Id;
        
        if (type == ConversationType.DIRECT && user1Id > user2Id) {
            normalizedUser1Id = user2Id;
            normalizedUser2Id = user1Id;
        }
        
        Conversation conversation = Conversation.builder()
            .user1Id(normalizedUser1Id)
            .user2Id(normalizedUser2Id)
            .conversationType(type)
            .chargerId(chargerId)
            .unreadCountUser1(0)
            .unreadCountUser2(0)
            .archivedUser1(false)
            .archivedUser2(false)
            .isActive(true)
            .build();
        
        // Set default title based on type
        if (type == ConversationType.USER_HOST && chargerId != null) {
            Charger charger = chargerRepository.findById(chargerId).orElse(null);
            if (charger != null) {
                conversation.setTitle("Chat about " + charger.getName());
            }
        } else if (type == ConversationType.USER_ADMIN) {
            conversation.setTitle("Support Chat");
        } else if (type == ConversationType.HOST_ADMIN) {
            conversation.setTitle("Host Support");
        }
        
        Conversation saved = conversationRepository.save(conversation);
        log.info("✅ Conversation created: ID={}", saved.getId());
        
        return saved;
    }
    
    /**
     * Update conversation metadata after new message
     */
    private void updateConversationMetadata(
        Conversation conversation,
        String messageContent,
        LocalDateTime messageTime,
        Long receiverId,
        Long senderId
    ) {
        // Update last message preview
        String preview = messageContent.length() > MESSAGE_PREVIEW_LENGTH
            ? messageContent.substring(0, MESSAGE_PREVIEW_LENGTH) + "..."
            : messageContent;
        
        conversation.setLastMessage(preview);
        conversation.setLastMessageTime(messageTime);
        conversation.setLastMessageSenderId(senderId);
        
        // Increment unread count for receiver
        conversation.incrementUnreadCount(receiverId);
        
        // Unarchive conversation for both participants if archived
        if (conversation.isArchivedForUser(receiverId)) {
            conversation.unarchiveForUser(receiverId);
        }
        if (conversation.isArchivedForUser(senderId)) {
            conversation.unarchiveForUser(senderId);
        }
        
        conversationRepository.save(conversation);
    }
    
    /**
     * Validate message request
     */
    private void validateMessageRequest(Long senderId, ChatMessageRequest request) {
        if (senderId == null) {
            throw new IllegalArgumentException("Sender ID cannot be null");
        }
        
        if (request.getReceiverId() == null) {
            throw new IllegalArgumentException("Receiver ID is required");
        }
        
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        
        if (request.getContent().length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException(
                "Message too long. Maximum length is " + MAX_MESSAGE_LENGTH + " characters"
            );
        }
    }
    
    /**
     * Load user or throw exception
     */
    private User loadUser(Long userId, String userType) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(userType + " not found"));
    }
    
    /**
     * Get conversation or throw exception
     */
    private Conversation getConversationOrThrow(Long conversationId) {
        return conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
    }
    
    /**
     * Validate user is participant in conversation
     */
    private void validateUserIsParticipant(Conversation conversation, Long userId) {
        if (!conversation.isParticipant(userId)) {
            throw new IllegalArgumentException("User is not a participant in this conversation");
        }
    }
    
    /**
     * Check if user is online
     */
    private boolean isUserOnline(Long userId) {
        return userPresenceRepository.isUserOnline(userId);
    }
    
    // CONTINUED IN NEXT FILE FOR MAPPING METHODS...// CONTINUATION OF ChatMessageServiceImpl.java - MAPPING METHODS
    
    /**
     * Map ChatMessage entity to ChatMessageResponse DTO
     */
    private ChatMessageResponse mapToMessageResponse(ChatMessage message, Long currentUserId) {
        boolean isSender = message.getSender().getUserId().equals(currentUserId);
        
        return ChatMessageResponse.builder()
            .id(message.getId())
            .conversationId(message.getConversation().getId())
            .senderId(message.getSender().getUserId())
            .senderName(message.getSender().getFirstName() + " " + message.getSender().getLastName())
            .receiverId(message.getReceiver().getUserId())
            .receiverName(message.getReceiver().getFirstName() + " " + message.getReceiver().getLastName())
            .content(message.getContent())
            .status(message.getStatus())
            .isSender(isSender)
            .isDeleted(message.getIsDeleted())
            .createdAt(message.getCreatedAt())
            .deliveredAt(message.getDeliveredAt())
            .readAt(message.getReadAt())
            .build();
    }
    
    /**
     * Map Conversation entity to ConversationResponse DTO
     */
    private ConversationResponse mapToConversationResponse(Conversation conversation, Long currentUserId) {
        Long otherUserId = conversation.getOtherUserId(currentUserId);
        User otherUser = loadUser(otherUserId, "Other User");
        
        // Get presence information
        UserPresence presence = userPresenceRepository.findByUserId(otherUserId).orElse(null);
        boolean isOnline = presence != null && presence.getStatus() == UserPresenceStatus.ONLINE;
        LocalDateTime lastSeen = presence != null ? presence.getLastSeenAt() : null;
        
        // Build participant info
        ConversationResponse.ParticipantInfo participantInfo = ConversationResponse.ParticipantInfo.builder()
            .userId(otherUser.getUserId())
            .firstName(otherUser.getFirstName())
            .lastName(otherUser.getLastName())
            .email(otherUser.getEmail())
            .role(otherUser.getRole().name())
            .isOnline(isOnline)
            .lastSeen(lastSeen)
            .build();
        
        // Build charger context if applicable
        ConversationResponse.ChargerContextInfo chargerContext = null;
        if (conversation.getConversationType() == ConversationType.USER_HOST 
            && conversation.getChargerId() != null) {
            
            Charger charger = chargerRepository.findById(conversation.getChargerId())
                .orElse(null);
            
            if (charger != null) {
                chargerContext = ConversationResponse.ChargerContextInfo.builder()
                    .chargerId(charger.getId())
                    .chargerName(charger.getName())
                    .chargerLocation(charger.getLocation())
                    .chargerImage(charger.getImages() != null && !charger.getImages().isEmpty() 
                        ? charger.getImages().get(0) 
                        : null)
                    .build();
            }
        }
        
        return ConversationResponse.builder()
            .id(conversation.getId())
            .otherParticipant(participantInfo)
            .conversationType(conversation.getConversationType())
            .chargerContext(chargerContext)
            .title(conversation.getTitle())
            .lastMessage(conversation.getLastMessage())
            .lastMessageSenderId(conversation.getLastMessageSenderId())
            .lastMessageTime(conversation.getLastMessageTime())
            .unreadCount(conversation.getUnreadCountForUser(currentUserId))
            .isArchived(conversation.isArchivedForUser(currentUserId))
            .isOtherParticipantOnline(isOnline)
            .otherParticipantLastSeen(lastSeen)
            .createdAt(conversation.getCreatedAt())
            .updatedAt(conversation.getUpdatedAt())
            .build();
    }
    
    /**
     * Map User to UserSearchResponse for admin chat search
     */
    private UserSearchResponse mapToUserSearchResponse(User user, Long adminId) {
        // Check if there's an existing conversation
        List<Conversation> existingConversations = conversationRepository
            .findByParticipants(user.getUserId(), adminId);
        
        Conversation adminConversation = existingConversations.stream()
            .filter(c -> c.getConversationType() == ConversationType.USER_ADMIN 
                      || c.getConversationType() == ConversationType.HOST_ADMIN)
            .findFirst()
            .orElse(null);
        
        // Get presence information
        UserPresence presence = userPresenceRepository.findByUserId(user.getUserId()).orElse(null);
        boolean isOnline = presence != null && presence.getStatus() == UserPresenceStatus.ONLINE;
        LocalDateTime lastSeen = presence != null ? presence.getLastSeenAt() : null;
        
        return UserSearchResponse.builder()
            .userId(user.getUserId())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .role(user.getRole().name())
            .isOnline(isOnline)
            .lastSeen(lastSeen)
            .createdAt(user.getCreatedAt())
            .hasExistingConversation(adminConversation != null)
            .existingConversationId(adminConversation != null ? adminConversation.getId() : null)
            .unreadCount(adminConversation != null 
                ? adminConversation.getUnreadCountForUser(adminId) 
                : 0)
            .build();
    }


    // ==================== ADDITIONAL METHODS FOR WEBSOCKET SUPPORT ====================
// Add these methods to ChatMessageServiceImpl.java

/**
 * Set user as online (called when WebSocket connects)
 */
@Override
@Transactional
public void setUserOnline(Long userId) {
    log.info("👋 Setting user {} status to ONLINE", userId);
    
    UserPresence presence = userPresenceRepository.findByUserId(userId)
        .orElse(UserPresence.builder()
            .userId(userId)
            .status(UserPresenceStatus.OFFLINE)
            .build());
    
    presence.setOnline(); // Sets status to ONLINE and updates lastSeenAt
    userPresenceRepository.save(presence);
    
    log.debug("✅ User {} is now ONLINE", userId);
}

/**
 * Set user as offline (called when WebSocket disconnects)
 */
@Override
@Transactional
public void setUserOffline(Long userId) {
    log.info("👋 Setting user {} status to OFFLINE", userId);
    
    UserPresence presence = userPresenceRepository.findByUserId(userId)
        .orElse(UserPresence.builder()
            .userId(userId)
            .status(UserPresenceStatus.ONLINE)
            .build());
    
    presence.setOffline(); // Sets status to OFFLINE and updates lastSeenAt
    userPresenceRepository.save(presence);
    
    log.debug("✅ User {} is now OFFLINE", userId);
}

/**
 * Mark pending messages as delivered when user comes online
 * This is called when a user connects via WebSocket
 * 
 * @param userId User ID who just came online
 * @return Number of messages marked as delivered
 */
@Override
@Transactional
public int markPendingMessagesAsDelivered(Long userId) {
    log.info("📬 Marking pending messages as delivered for user {}", userId);
    
    // Find all messages where:
    // 1. User is the receiver
    // 2. Status is SENT (not yet DELIVERED)
    // 3. Message is not deleted
    List<ChatMessage> pendingMessages = chatMessageRepository
        .findByReceiverUserIdAndStatusAndIsDeletedFalse(
            userId, 
            MessageStatus.SENT
        );
    
    if (pendingMessages.isEmpty()) {
        log.debug("No pending messages to deliver for user {}", userId);
        return 0;
    }
    
    // Mark all as delivered
    pendingMessages.forEach(ChatMessage::markAsDelivered);
    chatMessageRepository.saveAll(pendingMessages);
    
    int count = pendingMessages.size();
    log.info("✅ Marked {} messages as delivered for user {}", count, userId);
    
    return count;
}
}