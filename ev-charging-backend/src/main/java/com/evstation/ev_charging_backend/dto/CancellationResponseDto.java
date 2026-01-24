package com.evstation.ev_charging_backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationResponseDto {

    private boolean success;
    private Long bookingId;
    private String message;
    private boolean refundProcessed;
    private BigDecimal refundAmount;
    private String refundId;
    private LocalDateTime refundedAt;
}