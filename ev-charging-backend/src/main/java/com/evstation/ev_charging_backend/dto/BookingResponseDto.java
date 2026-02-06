package com.evstation.ev_charging_backend.dto;

import com.evstation.ev_charging_backend.enums.BookingStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDto {

    private Long bookingId;
    private Long userId;
    private Long chargerId;
    private String chargerName;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private BookingStatus status;
    private LocalDateTime reservedUntil; // For payment timer

    private BigDecimal pricePerKwh;
    private BigDecimal totalPrice;

    // ⭐ NEW - Rating information
    private Long ratingId;           // ID of the rating if exists
    private Integer ratingScore;     // Rating score (1-5) if exists
    private String ratingComment;    // Rating comment if exists
    private LocalDateTime ratingCreatedAt; // When the rating was created
}