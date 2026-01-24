package com.evstation.ev_charging_backend.exception;

public class CancellationDeadlinePassedException extends RuntimeException {
    public CancellationDeadlinePassedException(String message) {
        super(message);
    }
}