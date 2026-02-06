package com.evstation.ev_charging_backend.exception;

/**
 * Exception thrown when a rating cannot be found in the system.
 */
public class RatingNotFoundException extends RuntimeException {
    
    public RatingNotFoundException(String message) {
        super(message);
    }
    
    public RatingNotFoundException(Long ratingId) {
        super(String.format("Rating not found with ID: %d", ratingId));
    }
}