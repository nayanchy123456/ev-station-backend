package com.evstation.ev_charging_backend.exception;

/**
 * Exception thrown when a user attempts to modify or delete a rating
 * that doesn't belong to them.
 */
public class UnauthorizedRatingAccessException extends RuntimeException {
    
    public UnauthorizedRatingAccessException(String message) {
        super(message);
    }
    
    public UnauthorizedRatingAccessException() {
        super("You are not authorized to perform this action on this rating");
    }
}