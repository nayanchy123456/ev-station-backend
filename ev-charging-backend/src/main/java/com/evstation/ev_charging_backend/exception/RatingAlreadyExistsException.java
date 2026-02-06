package com.evstation.ev_charging_backend.exception;

/**
 * Exception thrown when a user attempts to create a rating for a booking
 * that already has a rating associated with it.
 */
public class RatingAlreadyExistsException extends RuntimeException {
    
    public RatingAlreadyExistsException(String message) {
        super(message);
    }
    
    public RatingAlreadyExistsException(Long bookingId) {
        super(String.format("Rating already exists for booking with ID: %d", bookingId));
    }
}