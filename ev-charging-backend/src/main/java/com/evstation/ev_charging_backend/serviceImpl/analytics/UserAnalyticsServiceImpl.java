package com.evstation.ev_charging_backend.serviceImpl.analytics;


import com.evstation.ev_charging_backend.dto.analytics.common.*;
import com.evstation.ev_charging_backend.dto.analytics.user.*;
import com.evstation.ev_charging_backend.repository.BookingRepository;
import com.evstation.ev_charging_backend.repository.RatingRepository;
import com.evstation.ev_charging_backend.service.analytics.UserAnalyticsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAnalyticsServiceImpl implements UserAnalyticsService {

    private final BookingRepository bookingRepository;
    private final RatingRepository ratingRepository;

    @Override
    @Cacheable(value = "userOverview", key = "#userId + '-' + #period")
    public UserOverviewDTO getOverview(Long userId, AnalyticsPeriod period) {
        log.info("Calculating overview for user {} with period {}", userId, period);
        
        LocalDateTime startDate = getStartDate(period);
        LocalDateTime previousStartDate = getPreviousStartDate(period);
        
        // Current period metrics
        Long totalBookings = bookingRepository.getTotalBookingsByUser(userId, startDate);
        BigDecimal totalSpent = bookingRepository.getTotalSpendingByUser(userId, startDate);
        Double avgSessionDuration = bookingRepository.getAverageSessionDuration(userId, startDate);
        Double totalEnergy = bookingRepository.getTotalEnergyConsumed(userId, startDate);
        Long completedBookings = getCompletedBookingsCount(userId, startDate);
        
        // Previous period for comparison
        BigDecimal previousSpending = bookingRepository.getTotalSpendingByUser(userId, previousStartDate);
        Long previousBookings = bookingRepository.getTotalBookingsByUser(userId, previousStartDate);
        
        // Get favorite charger
        UserOverviewDTO.FavoriteChargerInfo favoriteCharger = getFavoriteCharger(userId, startDate);
        
        // Calculate average spending per session
        BigDecimal avgSpending = totalBookings > 0
            ? totalSpent.divide(BigDecimal.valueOf(totalBookings), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        return UserOverviewDTO.builder()
            .totalBookings(totalBookings)
            .totalSpent(totalSpent)
            .favoriteCharger(favoriteCharger)
            .averageSessionDuration(avgSessionDuration != null 
                ? BigDecimal.valueOf(avgSessionDuration).setScale(2, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO)
            .spendingComparison(calculateComparison(totalSpent, previousSpending))
            .bookingsComparison(calculateComparison(
                BigDecimal.valueOf(totalBookings), 
                BigDecimal.valueOf(previousBookings)))
            .averageSpendingPerSession(avgSpending)
            .completedBookings(completedBookings)
            .totalEnergyConsumed(totalEnergy != null 
                ? BigDecimal.valueOf(totalEnergy).setScale(2, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO)
            .build();
    }

    @Override
    @Cacheable(value = "userSpending", key = "#userId + '-' + #period")
    public UserSpendingAnalyticsDTO getSpendingAnalytics(Long userId, AnalyticsPeriod period) {
        log.info("Calculating spending analytics for user {} with period {}", userId, period);
        
        LocalDateTime startDate = getStartDate(period);
        LocalDateTime previousStartDate = getPreviousStartDate(period);
        
        // Current and previous spending
        BigDecimal currentSpending = bookingRepository.getTotalSpendingByUser(userId, startDate);
        BigDecimal previousSpending = bookingRepository.getTotalSpendingByUser(userId, previousStartDate);
        
        // Daily spending data
        List<ChartDataPoint> dailySpending = getDailySpendingData(userId, startDate);
        
        // Spending by charger
        List<UserSpendingAnalyticsDTO.ChargerSpendingDTO> spendingByCharger = 
            getSpendingByChargerData(userId, startDate, currentSpending);
        
        // Calculate metrics
        Long totalSessions = bookingRepository.getTotalBookingsByUser(userId, startDate);
        BigDecimal avgSpending = totalSessions > 0
            ? currentSpending.divide(BigDecimal.valueOf(totalSessions), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        return UserSpendingAnalyticsDTO.builder()
            .currentPeriodSpending(currentSpending)
            .previousPeriodSpending(previousSpending)
            .spendingComparison(calculateComparison(currentSpending, previousSpending))
            .dailySpending(dailySpending)
            .spendingByCharger(spendingByCharger)
            .averageSpendingPerSession(avgSpending)
            .totalSessions(totalSessions)
            .build();
    }

    @Override
    @Cacheable(value = "userBehavior", key = "#userId + '-' + #period")
    public UserChargingBehaviorDTO getChargingBehavior(Long userId, AnalyticsPeriod period) {
        log.info("Calculating charging behavior for user {} with period {}", userId, period);
        
        LocalDateTime startDate = getStartDate(period);
        
        // Most visited chargers
        List<Object[]> visitedData = bookingRepository.getMostVisitedChargersByUser(userId, startDate);
        List<UserChargingBehaviorDTO.VisitedChargerDTO> mostVisited = new ArrayList<>();
        
        for (Object[] row : visitedData) {
            Long chargerId = (Long) row[0];
            String chargerName = (String) row[1];
            String brand = (String) row[2];
            String location = (String) row[3];
            Long visits = (Long) row[4];
            BigDecimal totalSpent = (BigDecimal) row[5];
            
            // Handle the timestamp properly - it might be Timestamp or LocalDateTime
            Object timestampObj = row[6];
            LocalDateTime lastVisit;
            
            if (timestampObj instanceof java.sql.Timestamp) {
                lastVisit = ((java.sql.Timestamp) timestampObj).toLocalDateTime();
            } else if (timestampObj instanceof LocalDateTime) {
                lastVisit = (LocalDateTime) timestampObj;
            } else {
                // Fallback to current time if type is unexpected
                log.warn("Unexpected timestamp type: {}", timestampObj != null ? timestampObj.getClass() : "null");
                lastVisit = LocalDateTime.now();
            }
            
            // Get average rating given to this charger
            Double avgRating = ratingRepository.getAverageRatingGivenToCharger(userId, chargerId);
            
            mostVisited.add(UserChargingBehaviorDTO.VisitedChargerDTO.builder()
                .chargerId(chargerId)
                .chargerName(chargerName)
                .brand(brand)
                .location(location)
                .visits(visits)
                .totalSpent(totalSpent)
                .averageRatingGiven(avgRating != null 
                    ? BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP) 
                    : BigDecimal.ZERO)
                .lastVisit(lastVisit)
                .build());
        }
        
        // Charging patterns
        UserChargingBehaviorDTO.ChargingPatternsDTO patterns = getChargingPatterns(userId, startDate);
        
        return UserChargingBehaviorDTO.builder()
            .mostVisitedChargers(mostVisited)
            .chargingPatterns(patterns)
            .build();
    }

    @Override
    @Cacheable(value = "userBookings", key = "#userId + '-' + #period")
    public UserBookingAnalyticsDTO getBookingAnalytics(Long userId, AnalyticsPeriod period) {
        log.info("Calculating booking analytics for user {} with period {}", userId, period);
        
        LocalDateTime startDate = getStartDate(period);
        
        // Get booking status distribution
        List<Object[]> statusData = bookingRepository.getBookingStatusDistributionByUser(userId, startDate);
        
        Long totalBookings = 0L;
        Long completedBookings = 0L;
        Long cancelledBookings = 0L;
        Long upcomingBookings = 0L;
        
        List<DistributionData> distribution = new ArrayList<>();
        
        for (Object[] row : statusData) {
            String status = row[0].toString();
            Long count = (Long) row[1];
            totalBookings += count;
            
            switch (status) {
                case "COMPLETED" -> completedBookings = count;
                case "CANCELLED" -> cancelledBookings = count;
                case "CONFIRMED", "ACTIVE", "RESERVED" -> upcomingBookings += count;
            }
        }
        
        // Calculate percentages
        final Long finalTotal = totalBookings;
        for (Object[] row : statusData) {
            String status = row[0].toString();
            Long count = (Long) row[1];
            BigDecimal percentage = finalTotal > 0
                ? BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(finalTotal), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            
            distribution.add(DistributionData.builder()
                .label(status)
                .count(count)
                .percentage(percentage)
                .build());
        }
        
        // Get recent bookings
        List<Object[]> recentData = bookingRepository.getRecentBookingsByUser(userId, PageRequest.of(0, 10));
        List<UserBookingAnalyticsDTO.RecentBookingDTO> recentBookings = recentData.stream()
            .map(row -> {
                // Handle timestamp - could be Timestamp or LocalDateTime
                Object dateObj = row[3];
                LocalDateTime date;
                
                if (dateObj instanceof java.sql.Timestamp) {
                    date = ((java.sql.Timestamp) dateObj).toLocalDateTime();
                } else if (dateObj instanceof LocalDateTime) {
                    date = (LocalDateTime) dateObj;
                } else {
                    log.warn("Unexpected date type in booking: {}", dateObj != null ? dateObj.getClass() : "null");
                    date = LocalDateTime.now();
                }
                
                // Handle duration - TIMESTAMPDIFF returns Long or Integer depending on DB
                Object durationObj = row[4];
                BigDecimal duration;
                
                if (durationObj instanceof Long) {
                    duration = BigDecimal.valueOf((Long) durationObj);
                } else if (durationObj instanceof Integer) {
                    duration = BigDecimal.valueOf((Integer) durationObj);
                } else if (durationObj instanceof BigDecimal) {
                    duration = (BigDecimal) durationObj;
                } else {
                    log.warn("Unexpected duration type: {}", durationObj != null ? durationObj.getClass() : "null");
                    duration = BigDecimal.ZERO;
                }
                
                // Handle status - could be BookingStatus enum or String
                Object statusObj = row[6];
                String status;
                
                if (statusObj instanceof String) {
                    status = (String) statusObj;
                } else {
                    status = statusObj.toString();
                }
                
                return UserBookingAnalyticsDTO.RecentBookingDTO.builder()
                    .bookingId((Long) row[0])
                    .chargerId((Long) row[1])
                    .chargerName((String) row[2])
                    .date(date)
                    .duration(duration)
                    .cost((BigDecimal) row[5])
                    .status(status)
                    .energyConsumed(row[7] != null 
                        ? BigDecimal.valueOf((Double) row[7]).setScale(2, RoundingMode.HALF_UP) 
                        : BigDecimal.ZERO)
                    .build();
            })
            .collect(Collectors.toList());
        
        return UserBookingAnalyticsDTO.builder()
            .totalBookings(totalBookings)
            .completedBookings(completedBookings)
            .cancelledBookings(cancelledBookings)
            .upcomingBookings(upcomingBookings)
            .bookingStatusDistribution(distribution)
            .recentBookings(recentBookings)
            .build();
    }

    @Override
    @Cacheable(value = "userRatings", key = "#userId")
    public UserRatingAnalyticsDTO getRatingAnalytics(Long userId) {
        log.info("Calculating rating analytics for user {}", userId);
        
        // Overall metrics
        Double avgRating = ratingRepository.getAverageRatingGivenByUser(userId);
        Long totalReviews = ratingRepository.getTotalReviewsByUser(userId);
        
        // Rating distribution
        RatingDistribution distribution = getUserRatingDistribution(userId, totalReviews);
        
        // Recent ratings
        List<Object[]> recentData = ratingRepository.getRecentRatingsByUser(userId, PageRequest.of(0, 10));
        List<UserRatingAnalyticsDTO.RecentRatingDTO> recentRatings = recentData.stream()
            .map(row -> UserRatingAnalyticsDTO.RecentRatingDTO.builder()
                .ratingId((Long) row[0])
                .chargerId((Long) row[1])
                .chargerName((String) row[2])
                .rating((Integer) row[3])
                .comment((String) row[4])
                .date((LocalDateTime) row[5])
                .build())
            .collect(Collectors.toList());
        
        return UserRatingAnalyticsDTO.builder()
            .averageRatingGiven(avgRating != null 
                ? BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO)
            .totalReviewsGiven(totalReviews)
            .ratingDistribution(distribution)
            .recentRatings(recentRatings)
            .build();
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private LocalDateTime getStartDate(AnalyticsPeriod period) {
        if (period == AnalyticsPeriod.ALL_TIME) {
            return LocalDateTime.of(2000, 1, 1, 0, 0);
        }
        return LocalDateTime.now().minusDays(period.getDays());
    }

    private LocalDateTime getPreviousStartDate(AnalyticsPeriod period) {
        if (period == AnalyticsPeriod.ALL_TIME) {
            return LocalDateTime.of(2000, 1, 1, 0, 0);
        }
        return LocalDateTime.now().minusDays(period.getDays() * 2);
    }

    private ComparisonData calculateComparison(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return ComparisonData.builder()
                .currentValue(current)
                .previousValue(previous != null ? previous : BigDecimal.ZERO)
                .changeAmount(current)
                .changePercentage(BigDecimal.ZERO)
                .isIncrease(current.compareTo(BigDecimal.ZERO) > 0)
                .build();
        }
        
        BigDecimal changeAmount = current.subtract(previous);
        BigDecimal changePercentage = changeAmount
            .multiply(BigDecimal.valueOf(100))
            .divide(previous, 2, RoundingMode.HALF_UP);
        
        return ComparisonData.builder()
            .currentValue(current)
            .previousValue(previous)
            .changeAmount(changeAmount)
            .changePercentage(changePercentage)
            .isIncrease(changeAmount.compareTo(BigDecimal.ZERO) > 0)
            .build();
    }

    private UserOverviewDTO.FavoriteChargerInfo getFavoriteCharger(Long userId, LocalDateTime startDate) {
        List<Object[]> favoriteData = bookingRepository.getMostVisitedChargersByUser(userId, startDate);
        
        if (favoriteData.isEmpty()) {
            return UserOverviewDTO.FavoriteChargerInfo.builder()
                .chargerId(null)
                .chargerName("N/A")
                .visits(0L)
                .build();
        }
        
        Object[] favorite = favoriteData.get(0);
        return UserOverviewDTO.FavoriteChargerInfo.builder()
            .chargerId((Long) favorite[0])
            .chargerName((String) favorite[1])
            .visits((Long) favorite[4])
            .build();
    }

    private Long getCompletedBookingsCount(Long userId, LocalDateTime startDate) {
        List<Object[]> statusData = bookingRepository.getBookingStatusDistributionByUser(userId, startDate);
        return statusData.stream()
            .filter(row -> "COMPLETED".equals(row[0].toString()))
            .mapToLong(row -> (Long) row[1])
            .sum();
    }

    private List<ChartDataPoint> getDailySpendingData(Long userId, LocalDateTime startDate) {
        List<Object[]> dailyData = bookingRepository.getDailySpendingByUser(userId, startDate);
        
        return dailyData.stream()
            .map(row -> {
                // Convert java.sql.Date to LocalDate
                java.sql.Date sqlDate = (java.sql.Date) row[0];
                LocalDate localDate = sqlDate.toLocalDate();
                
                return ChartDataPoint.builder()
                    .date(localDate)
                    .value((BigDecimal) row[1])
                    .count((Long) row[2])
                    .build();
            })
            .collect(Collectors.toList());
    }

    private List<UserSpendingAnalyticsDTO.ChargerSpendingDTO> getSpendingByChargerData(
            Long userId, LocalDateTime startDate, BigDecimal totalSpending) {
        
        List<Object[]> spendingData = bookingRepository.getSpendingByCharger(userId, startDate);
        
        return spendingData.stream()
            .map(row -> {
                Long chargerId = (Long) row[0];
                String chargerName = (String) row[1];
                BigDecimal spent = (BigDecimal) row[2];
                Long sessionCount = (Long) row[3];
                
                BigDecimal percentage = totalSpending.compareTo(BigDecimal.ZERO) > 0
                    ? spent.multiply(BigDecimal.valueOf(100))
                        .divide(totalSpending, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                
                return UserSpendingAnalyticsDTO.ChargerSpendingDTO.builder()
                    .chargerId(chargerId)
                    .chargerName(chargerName)
                    .spent(spent)
                    .percentage(percentage)
                    .sessionCount(sessionCount)
                    .build();
            })
            .collect(Collectors.toList());
    }

    private UserChargingBehaviorDTO.ChargingPatternsDTO getChargingPatterns(Long userId, LocalDateTime startDate) {
        Double avgDuration = bookingRepository.getAverageSessionDuration(userId, startDate);
        Double totalEnergy = bookingRepository.getTotalEnergyConsumed(userId, startDate);
        Long totalSessions = bookingRepository.getTotalBookingsByUser(userId, startDate);
        
        BigDecimal avgEnergy = totalSessions > 0 && totalEnergy != null
            ? BigDecimal.valueOf(totalEnergy).divide(BigDecimal.valueOf(totalSessions), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        // Get peak charging hour
        Integer peakHour = null;
        try {
            Object[] peakHourData = bookingRepository.getPeakChargingHour(userId, startDate);
            if (peakHourData != null && peakHourData.length > 0 && peakHourData[0] != null) {
                // Convert to Integer - handle different numeric types
                if (peakHourData[0] instanceof Number) {
                    peakHour = ((Number) peakHourData[0]).intValue();
                }
            }
        } catch (Exception e) {
            log.warn("Could not determine peak charging hour for user {}: {}", userId, e.getMessage());
        }
        
        // Get peak charging day
        String peakDay = null;
        try {
            Object[] peakDayData = bookingRepository.getPeakChargingDay(userId, startDate);
            if (peakDayData != null && peakDayData.length > 0 && peakDayData[0] != null) {
                peakDay = peakDayData[0].toString();
            }
        } catch (Exception e) {
            log.warn("Could not determine peak charging day for user {}: {}", userId, e.getMessage());
        }
        
        return UserChargingBehaviorDTO.ChargingPatternsDTO.builder()
            .averageSessionDuration(avgDuration != null 
                ? BigDecimal.valueOf(avgDuration).setScale(2, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO)
            .totalEnergyConsumed(totalEnergy != null 
                ? BigDecimal.valueOf(totalEnergy).setScale(2, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO)
            .peakChargingDay(peakDay)
            .peakChargingHour(peakHour)
            .averageEnergyPerSession(avgEnergy)
            .build();
    }

    private RatingDistribution getUserRatingDistribution(Long userId, Long totalReviews) {
        List<Object[]> ratingData = ratingRepository.getRatingDistributionByUser(userId);
        RatingDistribution distribution = new RatingDistribution();
        
        for (Object[] row : ratingData) {
            Integer stars = (Integer) row[0];
            Long count = (Long) row[1];
            BigDecimal percentage = totalReviews > 0
                ? BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalReviews), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            
            distribution.setRating(stars, count, percentage);
        }
        
        return distribution;
    }
}