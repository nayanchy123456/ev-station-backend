package com.evstation.ev_charging_backend.exception;

public class InvalidDateRangeException extends RuntimeException {
    
    public InvalidDateRangeException(String message) {
        super(message);
    }
    
    public InvalidDateRangeException(String message, Throwable cause) {
        super(message, cause);
    }
}