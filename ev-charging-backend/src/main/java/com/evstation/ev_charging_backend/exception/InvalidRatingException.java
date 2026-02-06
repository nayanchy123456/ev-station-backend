package com.evstation.ev_charging_backend.exception;

/**
 * Exception thrown when an invalid rating operation is attempted,
 * such as rating a booking that is not completed or belongs to another user.
 */
public class InvalidRatingException extends RuntimeException {
    
    public InvalidRatingException(String message) {
        super(message);
    }
}