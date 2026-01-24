package com.evstation.ev_charging_backend.enums;

public enum BookingStatus {
    RESERVED,          // Initial reservation (10 min window)
    PAYMENT_PENDING,   // Payment initiated but not completed
    CONFIRMED,         // Payment successful
    ACTIVE,            // Charging in progress
    COMPLETED,         // Charging finished
    CANCELLED,         // User cancelled
    EXPIRED            // Reservation timeout
}