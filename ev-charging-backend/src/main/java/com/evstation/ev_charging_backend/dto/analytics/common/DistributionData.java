package com.evstation.ev_charging_backend.dto.analytics.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for distribution data (used in pie charts, bar charts)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionData {
    private String label;
    private Long count;
    private Long value; // Alternative to count
    private BigDecimal percentage;
}