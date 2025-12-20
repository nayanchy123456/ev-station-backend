package com.evstation.ev_charging_backend.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDto {

    @NotNull(message = "Charger ID is required")
    private Long chargerId;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;
}