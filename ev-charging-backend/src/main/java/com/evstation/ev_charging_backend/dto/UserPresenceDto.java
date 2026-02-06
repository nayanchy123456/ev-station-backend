package com.evstation.ev_charging_backend.dto;

import com.evstation.ev_charging_backend.enums.UserPresenceStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for user presence information.
 * 
 * Used for:
 * - Real-time presence updates via WebSocket
 * - Displaying online/offline status in chat UI
 * - Last seen timestamps
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPresenceDto {
    
    /**
     * User ID
     */
    private Long userId;
    
    /**
     * Current presence status
     */
    private UserPresenceStatus status;
    
    /**
     * Whether user is online (convenience field)
     */
    private Boolean isOnline;
    
    /**
     * Last time the user was seen online
     */
    private LocalDateTime lastSeenAt;
    
    /**
     * When this presence record was last updated
     */
    private LocalDateTime updatedAt;
}
