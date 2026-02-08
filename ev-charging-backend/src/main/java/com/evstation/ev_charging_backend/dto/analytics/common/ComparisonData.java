package com.evstation.ev_charging_backend.dto.analytics.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for period-over-period comparison data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonData {
    private BigDecimal currentValue;
    private BigDecimal previousValue;
    private BigDecimal changeAmount;
    private BigDecimal changePercentage;
    private boolean isIncrease; // true if current > previous
}
