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
     * Sender information
     */
    private UserBasicInfo sender;
    
    /**
     * Receiver information
     */
    private UserBasicInfo receiver;
    
    /**
     * The actual message content
     */
    private String content;
    
    /**
     * Current status: SENT, DELIVERED, or READ
     */
    private MessageStatus status;
    
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
    
    /**
     * Whether this message has been deleted
     */
    private Boolean isDeleted;
    
    /**
     * Nested class for basic user information
     * Avoids circular references and reduces payload size
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserBasicInfo {
        private Long userId;
        private String firstName;
        private String lastName;
        private String email;
        
        /**
         * Get full name of user
         */
        public String getFullName() {
            return firstName + " " + lastName;
        }
    }
}