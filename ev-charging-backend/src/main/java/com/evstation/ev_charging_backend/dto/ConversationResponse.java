package com.evstation.ev_charging_backend.dto;

import com.evstation.ev_charging_backend.enums.UserPresenceStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for conversation list responses.
 * 
 * Used when:
 * - Fetching list of all conversations for a user
 * - Displaying conversation preview in sidebar
 * 
 * Contains:
 * - Basic conversation info
 * - Other participant details
 * - Last message preview
 * - Unread count
 * - Online status of other user
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponse {
    
    /**
     * Unique conversation ID
     */
    private Long conversationId;
    
    /**
     * Information about the other participant in the conversation
     */
    private ParticipantInfo otherUser;
    
    /**
     * Preview of the last message sent
     */
    private String lastMessage;
    
    /**
     * Timestamp of the last message
     * Used for sorting conversations (most recent first)
     */
    private LocalDateTime lastMessageTime;
    
    /**
     * Number of unread messages for the current user
     */
    private Integer unreadCount;
    
    /**
     * When this conversation was created
     */
    private LocalDateTime createdAt;
    
    /**
     * Nested class for participant information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ParticipantInfo {
        private Long userId;
        private String firstName;
        private String lastName;
        private String email;
        
        /**
         * Online status of the participant
         */
        private UserPresenceStatus presenceStatus;
        
        /**
         * Last time the participant was seen online
         */
        private LocalDateTime lastSeen;
        
        /**
         * Get full name
         */
        public String getFullName() {
            return firstName + " " + lastName;
        }
        
        /**
         * Check if user is currently online
         */
        public boolean isOnline() {
            return presenceStatus == UserPresenceStatus.ONLINE;
        }
    }
}