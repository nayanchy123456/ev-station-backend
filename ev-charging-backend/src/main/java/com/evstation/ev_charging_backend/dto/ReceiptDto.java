package com.evstation.ev_charging_backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptDto {

    private Long receiptId;
    private String receiptNumber;
    private Long bookingId;
    private Long paymentId;
    
    // Booking details
    private String chargerName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMinutes;
    
    // Payment details
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String transactionId;
    private LocalDateTime paymentDate;
    
    // User details
    private String userName;
    private String userEmail;
    
    private LocalDateTime generatedAt;
    private String pdfUrl;
}