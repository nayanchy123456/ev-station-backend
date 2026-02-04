package com.evstation.ev_charging_backend.enums;

/**
 * Represents the status of a chat message throughout its lifecycle.
 * 
 * Status Flow:
 * SENT -> DELIVERED -> READ
 * 
 * - SENT: Message created and stored in database
 * - DELIVERED: Message delivered to recipient (recipient is online)
 * - READ: Recipient has viewed the message
 */
public enum MessageStatus {
    /**
     * Message has been sent and saved to database
     */
    SENT,
    
    /**
     * Message has been delivered to the recipient
     * (Recipient's WebSocket connection received it)
     */
    DELIVERED,
    
    /**
     * Message has been read by the recipient
     */
    READ
}