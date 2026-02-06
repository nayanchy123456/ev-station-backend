package com.evstation.ev_charging_backend.dto;

import com.evstation.ev_charging_backend.enums.ConversationType;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for conversation responses.
 * 
 * Contains complete conversation information including:
 * - Conversation metadata
 * - Participant information
 * - Charger context (for USER_HOST conversations)
 * - Last message preview
 * - Unread counts
 * - Online status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponse {
    
    /**
     * Unique conversation ID
     */
    private Long id;
    
    /**
     * Information about the other participant in the conversation
     */
    private ParticipantInfo otherParticipant;
    
    /**
     * Type of conversation (DIRECT, USER_HOST, USER_ADMIN, HOST_ADMIN)
     */
    private ConversationType conversationType;
    
    /**
     * Charger context information (for USER_HOST conversations)
     */
    private ChargerContextInfo chargerContext;
    
    /**
     * Optional conversation title
     */
    private String title;
    
    /**
     * Preview of the last message sent
     */
    private String lastMessage;
    
    /**
     * ID of the user who sent the last message
     */
    private Long lastMessageSenderId;
    
    /**
     * Timestamp of the last message
     */
    private LocalDateTime lastMessageTime;
    
    /**
     * Number of unread messages for the current user
     */
    private Integer unreadCount;
    
    /**
     * Whether conversation is archived for current user
     */
    private Boolean isArchived;
    
    /**
     * Whether the other participant is currently online
     */
    private Boolean isOtherParticipantOnline;
    
    /**
     * Last seen timestamp of other participant
     */
    private LocalDateTime otherParticipantLastSeen;
    
    /**
     * When this conversation was created
     */
    private LocalDateTime createdAt;
    
    /**
     * When this conversation was last updated
     */
    private LocalDateTime updatedAt;
    
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
        private String role;
        private Boolean isOnline;
        private LocalDateTime lastSeen;
        
        /**
         * Get full name of participant
         */
        public String getFullName() {
            return firstName + " " + lastName;
        }
    }
    
    /**
     * Nested class for charger context (USER_HOST conversations)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChargerContextInfo {
        private Long chargerId;
        private String chargerName;
        private String chargerLocation;
        private String chargerImage;
    }
}
