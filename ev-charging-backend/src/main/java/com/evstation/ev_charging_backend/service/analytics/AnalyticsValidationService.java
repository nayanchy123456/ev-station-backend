package com.evstation.ev_charging_backend.service.analytics;

import com.evstation.ev_charging_backend.exception.InvalidDateRangeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class AnalyticsValidationService {
    
    private static final int MAX_DATE_RANGE_DAYS = 365;
    private static final int WARNING_DATE_RANGE_DAYS = 90;
    
    public void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            log.warn("Date validation failed: null dates provided");
            throw new InvalidDateRangeException("Start date and end date are required");
        }
        
        if (startDate.isAfter(endDate)) {
            log.warn("Date validation failed: start date {} is after end date {}", startDate, endDate);
            throw new InvalidDateRangeException("Start date must be before or equal to end date");
        }
        
        // Allow dates up to tomorrow to account for timezone differences
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (endDate.isAfter(tomorrow)) {
            log.warn("Date validation failed: end date {} is too far in the future", endDate);
            throw new InvalidDateRangeException("End date cannot be in the future");
        }
        
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > MAX_DATE_RANGE_DAYS) {
            log.warn("Date validation failed: date range {} days exceeds maximum of {} days", 
                    daysBetween, MAX_DATE_RANGE_DAYS);
            throw new InvalidDateRangeException(
                String.format("Date range cannot exceed %d days. Requested: %d days", 
                        MAX_DATE_RANGE_DAYS, daysBetween)
            );
        }
        
        if (daysBetween > WARNING_DATE_RANGE_DAYS) {
            log.info("Large date range requested: {} days (from {} to {})", 
                    daysBetween, startDate, endDate);
        }
        
        log.debug("Date range validation passed: {} to {} ({} days)", 
                startDate, endDate, daysBetween);
    }
    
    public void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }
        if (size > 100) {
            throw new IllegalArgumentException("Page size cannot exceed 100");
        }
    }
    
    public void validateLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }
        if (limit > 100) {
            throw new IllegalArgumentException("Limit cannot exceed 100");
        }
    }
}