package com.evstation.ev_charging_backend.dto;

import com.evstation.ev_charging_backend.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponseDto {

    private Long reservationId;
    private Long userId;
    private Long chargerId;
    private String chargerName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;
    private LocalDateTime reservedUntil;
    private BigDecimal estimatedPrice;
    private BigDecimal pricePerKwh;
    private Long durationMinutes;
}