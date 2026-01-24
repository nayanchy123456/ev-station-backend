package com.evstation.ev_charging_backend.enums;

public enum PaymentStatus {
    PENDING,    // Payment initiated but not completed
    SUCCESS,    // Payment completed successfully
    FAILED,     // Payment failed
    REFUNDED    // Payment refunded
}