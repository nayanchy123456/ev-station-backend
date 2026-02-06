package com.evstation.ev_charging_backend.entity;

import com.evstation.ev_charging_backend.enums.ConversationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Enhanced Conversation Entity with Support for Different Conversation Types
 * 
 * Key Features:
 * - Supports USER-HOST, USER-ADMIN, HOST-ADMIN conversations
 * - Unique constraint ensures only one conversation between two users of a specific type
 * - Tracks conversation context (e.g., related charger for USER-HOST chats)
 * - user1Id is always the smaller ID (normalized) for DIRECT conversations
 * - Tracks last message for conversation list display
 * 
 * Example:
 * - User 5 chats with Host 3 about Charger 10 → Conversation(user1Id=5, user2Id=3, type=USER_HOST, chargerId=10)
 * - User 5 contacts Admin for support → Conversation(user1Id=5, user2Id=1(admin), type=USER_ADMIN)
 */
@Entity
@Table(
    name = "conversations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_conversation_users_type_context",
            columnNames = {"user1_id", "user2_id", "conversation_type", "charger_id"}
        )
    },
    indexes = {
        @Index(name = "idx_user1", columnList = "user1_id"),
        @Index(name = "idx_user2", columnList = "user2_id"),
        @Index(name = "idx_conversation_type", columnList = "conversation_type"),
        @Index(name = "idx_charger", columnList = "charger_id"),
        @Index(name = "idx_last_message_time", columnList = "last_message_time"),
        @Index(name = "idx_user1_type", columnList = "user1_id, conversation_type"),
        @Index(name = "idx_user2_type", columnList = "user2_id, conversation_type")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * First participant in the conversation
     * For DIRECT: always the one with smaller ID
     * For typed conversations: can be any participant
     */
    @Column(name = "user1_id", nullable = false)
    private Long user1Id;
    
    /**
     * Second participant in the conversation
     * For DIRECT: always the one with larger ID
     * For typed conversations: can be any participant
     */
    @Column(name = "user2_id", nullable = false)
    private Long user2Id;
    
    /**
     * Type of conversation (USER_HOST, USER_ADMIN, HOST_ADMIN, DIRECT)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_type", nullable = false)
    @Builder.Default
    private ConversationType conversationType = ConversationType.DIRECT;
    
    /**
     * Reference to charger ID for USER_HOST conversations
     * Null for other conversation types
     */
    @Column(name = "charger_id")
    private Long chargerId;
    
    /**
     * Optional title/subject for the conversation
     * E.g., "Support Request", "Charger: Tesla Supercharger"
     */
    @Column(name = "title")
    private String title;
    
    /**
     * Preview of the last message sent in this conversation
     */
    @Column(name = "last_message", columnDefinition = "TEXT")
    private String lastMessage;
    
    /**
     * ID of user who sent the last message
     */
    @Column(name = "last_message_sender_id")
    private Long lastMessageSenderId;
    
    /**
     * Timestamp of the last message (used for sorting conversations)
     */
    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;
    
    /**
     * Number of unread messages for user1
     */
    @Column(name = "unread_count_user1", nullable = false)
    @Builder.Default
    private Integer unreadCountUser1 = 0;
    
    /**
     * Number of unread messages for user2
     */
    @Column(name = "unread_count_user2", nullable = false)
    @Builder.Default
    private Integer unreadCountUser2 = 0;
    
    /**
     * Flag to indicate if conversation is archived for user1
     */
    @Column(name = "archived_user1", nullable = false)
    @Builder.Default
    private Boolean archivedUser1 = false;
    
    /**
     * Flag to indicate if conversation is archived for user2
     */
    @Column(name = "archived_user2", nullable = false)
    @Builder.Default
    private Boolean archivedUser2 = false;
    
    /**
     * Flag to indicate if conversation is active
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Helper method to get the other user's ID
     * 
     * @param currentUserId The current user's ID
     * @return The other user's ID in the conversation
     */
    public Long getOtherUserId(Long currentUserId) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("Current user ID cannot be null");
        }
        return currentUserId.equals(user1Id) ? user2Id : user1Id;
    }
    
    /**
     * Helper method to get unread count for a specific user
     * 
     * @param userId The user's ID
     * @return Unread count for that user
     */
    public Integer getUnreadCountForUser(Long userId) {
        if (userId == null) {
            return 0;
        }
        return userId.equals(user1Id) ? unreadCountUser1 : unreadCountUser2;
    }
    
    /**
     * Increment unread count for a specific user
     * 
     * @param userId The user who should see the unread increment
     */
    public void incrementUnreadCount(Long userId) {
        if (userId == null) {
            return;
        }
        if (userId.equals(user1Id)) {
            this.unreadCountUser1++;
        } else if (userId.equals(user2Id)) {
            this.unreadCountUser2++;
        }
    }
    
    /**
     * Reset unread count for a specific user
     * 
     * @param userId The user whose unread count should be reset
     */
    public void resetUnreadCount(Long userId) {
        if (userId == null) {
            return;
        }
        if (userId.equals(user1Id)) {
            this.unreadCountUser1 = 0;
        } else if (userId.equals(user2Id)) {
            this.unreadCountUser2 = 0;
        }
    }
    
    /**
     * Check if user is participant in this conversation
     * 
     * @param userId User ID to check
     * @return true if user is participant
     */
    public boolean isParticipant(Long userId) {
        return userId != null && (userId.equals(user1Id) || userId.equals(user2Id));
    }
    
    /**
     * Check if conversation is archived for a specific user
     * 
     * @param userId User ID to check
     * @return true if archived for this user
     */
    public boolean isArchivedForUser(Long userId) {
        if (userId == null) {
            return false;
        }
        return userId.equals(user1Id) ? archivedUser1 : archivedUser2;
    }
    
    /**
     * Archive conversation for a specific user
     * 
     * @param userId User ID who is archiving
     */
    public void archiveForUser(Long userId) {
        if (userId == null) {
            return;
        }
        if (userId.equals(user1Id)) {
            this.archivedUser1 = true;
        } else if (userId.equals(user2Id)) {
            this.archivedUser2 = true;
        }
    }
    
    /**
     * Unarchive conversation for a specific user
     * 
     * @param userId User ID who is unarchiving
     */
    public void unarchiveForUser(Long userId) {
        if (userId == null) {
            return;
        }
        if (userId.equals(user1Id)) {
            this.archivedUser1 = false;
        } else if (userId.equals(user2Id)) {
            this.archivedUser2 = false;
        }
    }
}