package com.evstation.ev_charging_backend.dto;

import com.evstation.ev_charging_backend.enums.UserPresenceStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for user presence information.
 * 
 * Used when:
 * - Checking if a user is online
 * - Broadcasting presence updates
 * - Displaying user's online status in UI
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPresenceDto {
    
    /**
     * ID of the user
     */
    private Long userId;
    
    /**
     * Current presence status: ONLINE or OFFLINE
     */
    private UserPresenceStatus status;
    
    /**
     * Last time the user was seen online
     */
    private LocalDateTime lastSeen;
    
    /**
     * When this presence status was last updated
     */
    private LocalDateTime updatedAt;
    
    /**
     * Helper method to check if user is online
     */
    public boolean isOnline() {
        return status == UserPresenceStatus.ONLINE;
    }
}