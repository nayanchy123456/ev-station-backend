package com.evstation.ev_charging_backend.dto;

import com.evstation.ev_charging_backend.enums.ConversationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for sending a chat message.
 * 
 * 🔧 IMPROVED: Added validation annotations for better error handling
 * 🔧 ENHANCED: Added conversationType and chargerId for different chat contexts
 * 
 * Validation Rules:
 * - receiverId: Must not be null
 * - content: Must not be blank, between 1-5000 characters
 * - conversationType: Optional, defaults to DIRECT
 * - chargerId: Optional, required only for USER_HOST conversations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRequest {
    
    /**
     * ID of the user receiving the message.
     * Must be a valid user ID in the system.
     */
    @NotNull(message = "Receiver ID is required")
    private Long receiverId;
    
    /**
     * The message content.
     * Length constraint matches database column and application settings.
     */
    @NotBlank(message = "Message content cannot be blank")
    @Size(min = 1, max = 5000, message = "Message must be between 1 and 5000 characters")
    private String content;
    
    /**
     * Type of conversation: DIRECT, USER_HOST, USER_ADMIN, HOST_ADMIN
     * Optional - defaults to DIRECT if not specified
     */
    private ConversationType conversationType;
    
    /**
     * ID of the charger (for USER_HOST conversations)
     * Optional - only required when conversationType is USER_HOST
     */
    private Long chargerId;
}