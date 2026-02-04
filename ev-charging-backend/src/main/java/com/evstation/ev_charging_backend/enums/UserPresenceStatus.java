package com.evstation.ev_charging_backend.enums;

/**
 * Represents the online/offline status of a user.
 * 
 * Status Changes:
 * - ONLINE: User has active WebSocket connection
 * - OFFLINE: User disconnected or no active connection
 */
public enum UserPresenceStatus {
    /**
     * User is currently online and connected via WebSocket
     */
    ONLINE,
    
    /**
     * User is offline or disconnected
     */
    OFFLINE
}