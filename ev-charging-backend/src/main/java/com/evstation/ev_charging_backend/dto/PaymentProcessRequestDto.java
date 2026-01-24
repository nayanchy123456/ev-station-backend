package com.evstation.ev_charging_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProcessRequestDto {

    @NotNull(message = "Payment ID is required")
    private Long paymentId;

    @NotNull(message = "Confirmation is required")
    private Boolean confirmPayment;
}