package com.evstation.ev_charging_backend.dto;

import com.evstation.ev_charging_backend.enums.ConversationType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * DTO for initiating a new conversation or sending a message to existing one
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationInitiateRequest {
    
    /**
     * ID of the user to chat with
     */
    private Long participantId;
    
    /**
     * Type of conversation (USER_HOST, USER_ADMIN, HOST_ADMIN, DIRECT)
     * Optional - defaults to DIRECT if not specified
     */
    private ConversationType conversationType;
    
    /**
     * Charger ID - Required for USER_HOST conversations
     * Optional for other types
     */
    private Long chargerId;
    
    /**
     * Initial message content (optional)
     * If provided, a message will be sent immediately after conversation creation
     */
    private String initialMessage;
    
    /**
     * Optional title for the conversation
     */
    private String title;
}