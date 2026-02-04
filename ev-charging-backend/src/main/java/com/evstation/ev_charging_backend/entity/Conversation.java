package com.evstation.ev_charging_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a one-on-one conversation between two users.
 * 
 * Key Features:
 * - Unique constraint ensures only one conversation between two users
 * - user1Id is always the smaller ID (normalized)
 * - Tracks last message for conversation list display
 * 
 * Example:
 * User 5 chats with User 3 → Conversation(user1Id=3, user2Id=5)
 */
@Entity
@Table(
    name = "conversations",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"user1_id", "user2_id"}
    ),
    indexes = {
        @Index(name = "idx_user1", columnList = "user1_id"),
        @Index(name = "idx_user2", columnList = "user2_id"),
        @Index(name = "idx_last_message_time", columnList = "last_message_time")
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
     * First user in the conversation (always the one with smaller ID)
     */
    @Column(name = "user1_id", nullable = false)
    private Long user1Id;
    
    /**
     * Second user in the conversation (always the one with larger ID)
     */
    @Column(name = "user2_id", nullable = false)
    private Long user2Id;
    
    /**
     * Preview of the last message sent in this conversation
     */
    @Column(name = "last_message", columnDefinition = "TEXT")
    private String lastMessage;
    
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
        return currentUserId.equals(user1Id) ? user2Id : user1Id;
    }
    
    /**
     * Helper method to get unread count for a specific user
     * 
     * @param userId The user's ID
     * @return Unread count for that user
     */
    public Integer getUnreadCountForUser(Long userId) {
        return userId.equals(user1Id) ? unreadCountUser1 : unreadCountUser2;
    }
    
    /**
     * Increment unread count for a specific user
     * 
     * @param userId The user who should see the unread increment
     */
    public void incrementUnreadCount(Long userId) {
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
        if (userId.equals(user1Id)) {
            this.unreadCountUser1 = 0;
        } else if (userId.equals(user2Id)) {
            this.unreadCountUser2 = 0;
        }
    }
}