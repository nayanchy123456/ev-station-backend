package com.evstation.ev_charging_backend.entity;

import com.evstation.ev_charging_backend.enums.MessageStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a single chat message within a conversation.
 * 
 * Features:
 * - Stores message content and metadata
 * - Tracks message status (SENT, DELIVERED, READ)
 * - Supports soft deletion
 * - Links to conversation and users
 */
@Entity
@Table(
    name = "chat_messages",
    indexes = {
        @Index(name = "idx_conversation", columnList = "conversation_id"),
        @Index(name = "idx_sender", columnList = "sender_id"),
        @Index(name = "idx_receiver", columnList = "receiver_id"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Reference to the conversation this message belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
    
    /**
     * User who sent this message
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    
    /**
     * User who receives this message
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;
    
    /**
     * The actual message content
     * Using TEXT type to support longer messages
     */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    /**
     * Current status of the message
     * SENT -> DELIVERED -> READ
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;
    
    /**
     * Soft delete flag
     * When true, message is hidden but not removed from database
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
    
    /**
     * Timestamp when message was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when message was delivered to recipient
     */
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    
    /**
     * Timestamp when message was read by recipient
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    /**
     * Mark message as delivered
     */
    public void markAsDelivered() {
        this.status = MessageStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }
    
    /**
     * Mark message as read
     */
    public void markAsRead() {
        this.status = MessageStatus.READ;
        this.readAt = LocalDateTime.now();
        // If not yet delivered, mark as delivered too
        if (this.deliveredAt == null) {
            this.deliveredAt = LocalDateTime.now();
        }
    }
    
    /**
     * Check if message is read
     */
    public boolean isRead() {
        return this.status == MessageStatus.READ;
    }
    
    /**
     * Soft delete the message
     */
    public void softDelete() {
        this.isDeleted = true;
    }
}