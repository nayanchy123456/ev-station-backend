package com.evstation.ev_charging_backend.dto.analytics.common;

/**
 * Enum representing different time periods for analytics queries
 */
public enum AnalyticsPeriod {
    LAST_7_DAYS(7),
    LAST_30_DAYS(30),
    ALL_TIME(-1); // -1 indicates no time limit

    private final int days;

    AnalyticsPeriod(int days) {
        this.days = days;
    }

    public int getDays() {
        return days;
    }

    /**
     * Parse string to AnalyticsPeriod enum
     * Defaults to LAST_7_DAYS if invalid
     */
    public static AnalyticsPeriod fromString(String period) {
        if (period == null) {
            return LAST_7_DAYS;
        }
        try {
            return AnalyticsPeriod.valueOf(period.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LAST_7_DAYS;
        }
    }
}
