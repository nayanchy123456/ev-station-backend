package com.evstation.ev_charging_backend.service;

import com.evstation.ev_charging_backend.dto.*;

public interface PaymentService {
    
    PaymentInitiateResponseDto initiatePayment(PaymentInitiateRequestDto dto, Long userId);
    
    PaymentProcessResponseDto processPayment(PaymentProcessRequestDto dto, Long userId);
    
    CancellationResponseDto processRefund(Long paymentId, Long userId, String reason);
    
    PaymentProcessResponseDto getPaymentById(Long paymentId, Long userId);
    
    PaymentProcessResponseDto getPaymentByBooking(Long bookingId, Long userId);
}