package com.evstation.ev_charging_backend.dto;

import com.evstation.ev_charging_backend.enums.MessageStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for chat message responses.
 * 
 * Used when:
 * - Sending message back to client via WebSocket
 * - Returning message history via REST API
 * - Real-time message delivery
 * 
 * Contains all necessary information for displaying a message in the UI
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {
    
    /**
     * Unique message ID
     */
    private Long id;
    
    /**
     * ID of the conversation this message belongs to
     */
    private Long conversationId;
    
    /**
     * Sender ID
     */
    private Long senderId;
    
    /**
     * Sender's full name
     */
    private String senderName;
    
    /**
     * Receiver ID
     */
    private Long receiverId;
    
    /**
     * Receiver's full name
     */
    private String receiverName;
    
    /**
     * The actual message content
     */
    private String content;
    
    /**
     * Current status: SENT, DELIVERED, or READ
     */
    private MessageStatus status;
    
    /**
     * Whether the current user is the sender
     */
    private Boolean isSender;
    
    /**
     * Whether this message has been deleted
     */
    private Boolean isDeleted;
    
    /**
     * When the message was created
     */
    private LocalDateTime createdAt;
    
    /**
     * When the message was delivered (if applicable)
     */
    private LocalDateTime deliveredAt;
    
    /**
     * When the message was read (if applicable)
     */
    private LocalDateTime readAt;
}