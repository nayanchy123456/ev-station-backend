package com.evstation.ev_charging_backend.service;

import com.evstation.ev_charging_backend.dto.ReceiptDto;
import com.evstation.ev_charging_backend.entity.Booking;
import com.evstation.ev_charging_backend.entity.Payment;

public interface ReceiptService {
    
    ReceiptDto generateReceipt(Booking booking, Payment payment);
    
    ReceiptDto getReceiptById(Long receiptId, Long userId);
    
    ReceiptDto getReceiptByBooking(Long bookingId, Long userId);
    
    byte[] downloadReceiptPdf(Long receiptId, Long userId);
}