package com.evstation.ev_charging_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for user search results in admin chat interface
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSearchResponse {
    
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String role;
    private Boolean isOnline;
    private LocalDateTime lastSeen;
    private LocalDateTime createdAt;
    
    /**
     * Whether there's an existing conversation with this user
     */
    private Boolean hasExistingConversation;
    
    /**
     * Existing conversation ID if any
     */
    private Long existingConversationId;
    
    /**
     * Unread message count in existing conversation
     */
    private Integer unreadCount;
}