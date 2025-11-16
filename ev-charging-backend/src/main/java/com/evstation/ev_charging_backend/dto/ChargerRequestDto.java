package com.evstation.ev_charging_backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ChargerRequestDto {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Brand is required")
    private String brand;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Price per kWh is required")
    @DecimalMin(value = "0.01", message = "Price per kWh must be greater than 0")
    private BigDecimal pricePerKwh;

    private List<String> images; // optional
}
