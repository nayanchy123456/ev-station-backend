package com.evstation.ev_charging_backend.dto.analytics.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Generic DTO for chart data points (used in line charts, bar charts, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartDataPoint {
    private LocalDate date;
    private BigDecimal value;
    private Long count;
    private String label;
}
