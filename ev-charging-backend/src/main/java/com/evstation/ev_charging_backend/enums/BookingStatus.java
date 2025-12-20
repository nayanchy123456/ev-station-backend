package com.evstation.ev_charging_backend.enums;

public enum BookingStatus {
    CONFIRMED,   // Booking created, waiting for start time
    ACTIVE,      // Charging session in progress
    COMPLETED,   // Charging session finished
    CANCELLED    // User cancelled the booking
}