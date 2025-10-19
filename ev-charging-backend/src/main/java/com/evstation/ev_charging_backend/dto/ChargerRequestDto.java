package com.evstation.ev_charging_backend.dto;

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
    private String location; // user enters location string

    @NotNull(message = "Price per kWh is required")
    private BigDecimal pricePerKwh;

    private List<String> images;
}
