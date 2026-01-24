package com.evstation.ev_charging_backend.dto;

import com.evstation.ev_charging_backend.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProcessResponseDto {

    private boolean success;
    private Long paymentId;
    private String transactionId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private String failureReason;
    private LocalDateTime completedAt;
    private ReceiptDto receipt;
    private String message;
}