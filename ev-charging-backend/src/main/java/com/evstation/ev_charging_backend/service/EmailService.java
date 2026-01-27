package com.evstation.ev_charging_backend.service;

import com.evstation.ev_charging_backend.entity.Booking;
import com.evstation.ev_charging_backend.entity.Payment;
import com.evstation.ev_charging_backend.entity.User;

public interface EmailService {
    
    void sendBookingConfirmation(User user, Booking booking, Payment payment);
    
    void sendPaymentSuccess(User user, Payment payment, Booking booking);
    
    void sendPaymentFailure(User user, Payment payment, String reason);
    
    void sendCancellationConfirmation(User user, Booking booking, Payment payment);
    
    void sendHostBookingNotification(User host, Booking booking);
    
    void sendReceiptEmail(User user, Booking booking, Payment payment);
}