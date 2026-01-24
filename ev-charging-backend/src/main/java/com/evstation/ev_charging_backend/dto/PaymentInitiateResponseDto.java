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
public class PaymentInitiateResponseDto {

    private Long paymentId;
    private Long bookingId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String paymentMethod;
    private LocalDateTime expiresAt;
    private String message;
}