package com.evstation.ev_charging_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiateRequestDto {

    @NotNull(message = "Reservation ID is required")
    private Long reservationId;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    private String remarks;
}