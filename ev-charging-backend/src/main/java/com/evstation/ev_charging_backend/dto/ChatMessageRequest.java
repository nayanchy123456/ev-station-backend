package com.evstation.ev_charging_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for sending a chat message.
 * 
 * 🔧 IMPROVED: Added validation annotations for better error handling
 * 
 * Validation Rules:
 * - receiverId: Must not be null
 * - content: Must not be blank, between 1-5000 characters
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
}