package com.evstation.ev_charging_backend.enums;

/**
 * Enum to categorize different types of conversations in the system
 * 
 * USER_HOST: Conversation between a user and a host (related to charger)
 * USER_ADMIN: Support conversation between user and admin
 * HOST_ADMIN: Support conversation between host and admin
 * DIRECT: General direct conversation between any two users
 */
public enum ConversationType {
    /**
     * Conversation between a user and the host of a charger
     * Initiated when user views/selects a specific charger
     */
    USER_HOST,
    
    /**
     * Support conversation between a user and admin
     */
    USER_ADMIN,
    
    /**
     * Support conversation between a host and admin
     */
    HOST_ADMIN,
    
    /**
     * General direct conversation (default)
     */
    DIRECT
}