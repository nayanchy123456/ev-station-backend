package com.evstation.ev_charging_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ChargerResponseDto {

    private Long id;
    private String name;
    private String brand;
    private String location; // location string
    private Double latitude; // auto-generated from location
    private Double longitude; // auto-generated from location
    private BigDecimal pricePerKwh;
    private List<String> images;
    private Long hostId;
    private String hostEmail;
    private Double rating; // default 0.0
}
