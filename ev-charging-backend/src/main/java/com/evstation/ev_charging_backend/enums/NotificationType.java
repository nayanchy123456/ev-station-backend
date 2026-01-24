package com.evstation.ev_charging_backend.enums;

public enum NotificationType {
    BOOKING_CONFIRMED,
    BOOKING_CANCELLED,
    BOOKING_ACTIVE,
    BOOKING_COMPLETED,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    REFUND_PROCESSED,
    RESERVATION_EXPIRING,  // Optional: 2 min warning
    RESERVATION_EXPIRED
}