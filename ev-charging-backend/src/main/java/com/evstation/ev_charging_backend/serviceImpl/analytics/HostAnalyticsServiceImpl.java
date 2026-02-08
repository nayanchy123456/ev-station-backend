package com.evstation.ev_charging_backend.serviceImpl.analytics;

import com.evstation.ev_charging_backend.dto.analytics.common.*;
import com.evstation.ev_charging_backend.dto.analytics.host.*;
import com.evstation.ev_charging_backend.repository.BookingRepository;
import com.evstation.ev_charging_backend.repository.ChargerRepository;
import com.evstation.ev_charging_backend.repository.RatingRepository;
import com.evstation.ev_charging_backend.service.analytics.HostAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HostAnalyticsServiceImpl implements HostAnalyticsService {

    private final BookingRepository bookingRepository;
    private final ChargerRepository chargerRepository;
    private final RatingRepository ratingRepository;

    @Override
    @Cacheable(value = "hostOverview", key = "#hostId + '-' + #period")
    public HostOverviewDTO getOverview(Long hostId, AnalyticsPeriod period) {
        log.info("Calculating overview for host {} with period {}", hostId, period);
        
        LocalDateTime startDate = getStartDate(period);
        LocalDateTime previousStartDate = getPreviousStartDate(period);
        
        // Current period metrics
        Long totalChargers = chargerRepository.getTotalChargersByHost(hostId);
        Long activeUsers = bookingRepository.getActiveUserCountByHost(hostId, startDate);
        BigDecimal totalRevenue = bookingRepository.getTotalRevenueByHost(hostId, startDate);
        Double avgRating = ratingRepository.getAverageRatingByHost(hostId);
        Long totalBookings = bookingRepository.getTotalBookingsByHost(hostId, startDate);
        Long completedBookings = bookingRepository.getCompletedBookingsByHost(hostId, startDate);
        
        // Previous period metrics for comparison
        BigDecimal previousRevenue = bookingRepository.getTotalRevenueByHost(hostId, previousStartDate);
        Long previousActiveUsers = bookingRepository.getActiveUserCountByHost(hostId, previousStartDate);
        Long previousBookings = bookingRepository.getTotalBookingsByHost(hostId, previousStartDate);
        
        // Calculate completion rate
        BigDecimal completionRate = totalBookings > 0 
            ? BigDecimal.valueOf(completedBookings).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalBookings), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        return HostOverviewDTO.builder()
            .totalChargers(totalChargers)
            .activeUsers(activeUsers)
            .totalRevenue(totalRevenue)
            .averageRating(avgRating != null ? BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
            .revenueComparison(calculateComparison(totalRevenue, previousRevenue))
            .activeUsersComparison(calculateComparison(
                BigDecimal.valueOf(activeUsers), 
                BigDecimal.valueOf(previousActiveUsers)))
            .bookingsComparison(calculateComparison(
                BigDecimal.valueOf(totalBookings), 
                BigDecimal.valueOf(previousBookings)))
            .totalBookings(totalBookings)
            .completedBookings(completedBookings)
            .completionRate(completionRate)
            .build();
    }

    @Override
    @Cacheable(value = "hostRevenue", key = "#hostId + '-' + #period")
    public HostRevenueAnalyticsDTO getRevenueAnalytics(Long hostId, AnalyticsPeriod period) {
        log.info("Calculating revenue analytics for host {} with period {}", hostId, period);
        
        LocalDateTime startDate = getStartDate(period);
        LocalDateTime previousStartDate = getPreviousStartDate(period);
        
        // Current and previous period revenue
        BigDecimal currentRevenue = bookingRepository.getTotalRevenueByHost(hostId, startDate);
        BigDecimal previousRevenue = bookingRepository.getTotalRevenueByHost(hostId, previousStartDate);
        
        // Daily revenue data
        List<ChartDataPoint> dailyRevenue = getDailyRevenueData(hostId, startDate);
        
        // Revenue by charger
        List<HostRevenueAnalyticsDTO.ChargerRevenueDTO> revenueByCharger = 
            getRevenueByChargerData(hostId, startDate, currentRevenue);
        
        // Period breakdowns
        HostRevenueAnalyticsDTO.PeriodBreakdown todayBreakdown = 
            getPeriodBreakdown(hostId, "Today", 1);
        HostRevenueAnalyticsDTO.PeriodBreakdown weekBreakdown = 
            getPeriodBreakdown(hostId, "This Week", 7);
        HostRevenueAnalyticsDTO.PeriodBreakdown monthBreakdown = 
            getPeriodBreakdown(hostId, "This Month", 30);
        
        return HostRevenueAnalyticsDTO.builder()
            .currentPeriodRevenue(currentRevenue)
            .previousPeriodRevenue(previousRevenue)
            .revenueComparison(calculateComparison(currentRevenue, previousRevenue))
            .dailyRevenue(dailyRevenue)
            .revenueByCharger(revenueByCharger)
            .todayBreakdown(todayBreakdown)
            .weekBreakdown(weekBreakdown)
            .monthBreakdown(monthBreakdown)
            .build();
    }

    @Override
    @Cacheable(value = "hostChargers", key = "#hostId + '-' + #period")
    public HostChargerAnalyticsDTO getChargerAnalytics(Long hostId, AnalyticsPeriod period) {
        log.info("=== START: getChargerAnalytics for hostId={}, period={} ===", hostId, period);
        
        try {
            LocalDateTime startDate = getStartDate(period);
            log.debug("Start date calculated: {}", startDate);
            
            // Get all chargers with performance data
            log.debug("Fetching charger data...");
            List<Object[]> chargerData = chargerRepository.getAllChargersByHost(hostId);
            log.info("Found {} chargers for host {}", chargerData != null ? chargerData.size() : 0, hostId);
            
            if (chargerData == null || chargerData.isEmpty()) {
                log.warn("No chargers found for host {}. Returning empty analytics.", hostId);
                return HostChargerAnalyticsDTO.builder()
                    .chargers(Collections.emptyList())
                    .topChargersByBookings(Collections.emptyList())
                    .topChargersByRevenue(Collections.emptyList())
                    .build();
            }
            
            log.debug("Fetching last booking dates...");
            List<Object[]> lastBookingDates = chargerRepository.getChargersWithLastBookingDate(hostId);
            log.info("Found {} last booking date records", lastBookingDates != null ? lastBookingDates.size() : 0);
            
            // Build last booking map with extensive logging
            log.debug("Building last booking map...");
            Map<Long, LocalDateTime> lastBookingMap = new HashMap<>();
            
            if (lastBookingDates != null) {
                for (Object[] row : lastBookingDates) {
                    try {
                        if (row == null || row.length < 2) {
                            log.warn("Skipping invalid last booking row: {}", Arrays.toString(row));
                            continue;
                        }
                        
                        Long chargerId = row[0] != null ? ((Number) row[0]).longValue() : null;
                        
                        if (chargerId == null) {
                            log.warn("Skipping row with null charger ID");
                            continue;
                        }
                        
                        LocalDateTime lastBooking = null;
                        if (row[1] != null) {
                            Object dateObj = row[1];
                            if (dateObj instanceof java.sql.Timestamp) {
                                lastBooking = ((java.sql.Timestamp) dateObj).toLocalDateTime();
                            } else if (dateObj instanceof LocalDateTime) {
                                lastBooking = (LocalDateTime) dateObj;
                            } else {
                                log.warn("Unexpected date type for charger {}: {}", chargerId, dateObj.getClass());
                            }
                        }
                        
                        lastBookingMap.put(chargerId, lastBooking);
                        log.debug("Charger {} last booking: {}", chargerId, lastBooking);
                        
                    } catch (Exception e) {
                        log.error("Error processing last booking row: {}", Arrays.toString(row), e);
                    }
                }
            }
            
            log.info("Last booking map built with {} entries", lastBookingMap.size());
            
            List<HostChargerAnalyticsDTO.ChargerPerformanceDTO> chargers = new ArrayList<>();
            
            // Process each charger
            int chargerIndex = 0;
            for (Object[] row : chargerData) {
                chargerIndex++;
                try {
                    log.debug("--- Processing charger #{} ---", chargerIndex);
                    
                    if (row == null || row.length < 5) {
                        log.error("Invalid charger row #{}: {}", chargerIndex, Arrays.toString(row));
                        continue;
                    }
                    
                    Long chargerId = row[0] != null ? ((Number) row[0]).longValue() : null;
                    String name = row[1] != null ? row[1].toString() : "Unknown";
                    String brand = row[2] != null ? row[2].toString() : "Unknown";
                    String location = row[3] != null ? row[3].toString() : "Unknown";
                    Double rating = row[4] != null ? ((Number) row[4]).doubleValue() : null;
                    
                    if (chargerId == null) {
                        log.error("Charger #{} has null ID, skipping", chargerIndex);
                        continue;
                    }
                    
                    log.debug("Charger {}: name='{}', brand='{}', location='{}', rating={}", 
                        chargerId, name, brand, location, rating);
                    
                    // Get performance metrics with null safety
                    log.debug("Fetching booking count for charger {}...", chargerId);
                    List<Object[]> allBookingCounts = bookingRepository.getBookingCountsByCharger(hostId, startDate);
                    Object[] bookingCount = allBookingCounts != null ? allBookingCounts.stream()
                        .filter(r -> r != null && r.length > 0 && r[0] != null && r[0].equals(chargerId))
                        .findFirst()
                        .orElse(new Object[]{chargerId, 0L})
                        : new Object[]{chargerId, 0L};
                    
                    log.debug("Fetching revenue for charger {}...", chargerId);
                    List<Object[]> allRevenue = bookingRepository.getRevenueByCharger(hostId, startDate);
                    Object[] revenueData = allRevenue != null ? allRevenue.stream()
                        .filter(r -> r != null && r.length > 0 && r[0] != null && r[0].equals(chargerId))
                        .findFirst()
                        .orElse(new Object[]{chargerId, name, BigDecimal.ZERO, 0L})
                        : new Object[]{chargerId, name, BigDecimal.ZERO, 0L};
                    
                    // Get rating data
                    log.debug("Fetching rating count for charger {}...", chargerId);
                    Long ratingCount = ratingRepository.getRatingCountByCharger(chargerId);
                    log.debug("Rating count: {}", ratingCount);
                    
                    log.debug("Fetching rating distribution for charger {}...", chargerId);
                    RatingDistribution ratingDist = getRatingDistributionForCharger(chargerId);
                    
                    // Get frequent users
                    log.debug("Fetching frequent users for charger {}...", chargerId);
                    List<HostChargerAnalyticsDTO.FrequentUserDTO> frequentUsers = 
                        getFrequentUsersForCharger(hostId, chargerId, startDate);
                    log.debug("Found {} frequent users", frequentUsers != null ? frequentUsers.size() : 0);
                    
                    // Safely extract values with null checks
                    Long totalBookings = 0L;
                    if (bookingCount != null && bookingCount.length > 1 && bookingCount[1] != null) {
                        try {
                            totalBookings = ((Number) bookingCount[1]).longValue();
                        } catch (Exception e) {
                            log.error("Error casting booking count for charger {}: {}", chargerId, bookingCount[1], e);
                        }
                    }
                    
                    BigDecimal revenue = BigDecimal.ZERO;
                    if (revenueData != null && revenueData.length > 2 && revenueData[2] != null) {
                        try {
                            revenue = (BigDecimal) revenueData[2];
                        } catch (ClassCastException e) {
                            log.error("Error casting revenue for charger {}: {}", chargerId, revenueData[2], e);
                            if (revenueData[2] instanceof Number) {
                                revenue = BigDecimal.valueOf(((Number) revenueData[2]).doubleValue());
                            }
                        }
                    }
                    
                    log.debug("Charger {} metrics: bookings={}, revenue={}", chargerId, totalBookings, revenue);
                    
                    HostChargerAnalyticsDTO.ChargerPerformanceDTO chargerDTO = HostChargerAnalyticsDTO.ChargerPerformanceDTO.builder()
                        .chargerId(chargerId)
                        .chargerName(name)
                        .brand(brand)
                        .location(location)
                        .totalBookings(totalBookings)
                        .revenue(revenue)
                        .averageRating(rating != null ? BigDecimal.valueOf(rating).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                        .ratingCount(ratingCount != null ? ratingCount : 0L)
                        .ratingDistribution(ratingDist)
                        .mostFrequentUsers(frequentUsers != null ? frequentUsers : Collections.emptyList())
                        .status("Active")
                        .lastBookingDate(lastBookingMap.get(chargerId))
                        .build();
                    
                    chargers.add(chargerDTO);
                    log.debug("Successfully processed charger {}", chargerId);
                    
                } catch (Exception e) {
                    log.error("Error processing charger #{} data for row: {}", chargerIndex, Arrays.toString(row), e);
                    // Continue processing other chargers
                }
            }
            
            log.info("Successfully processed {} out of {} chargers", chargers.size(), chargerData.size());
            
            // Sort for top performers
            log.debug("Sorting top performers...");
            List<HostChargerAnalyticsDTO.ChargerPerformanceDTO> topByBookings = chargers.stream()
                .sorted(Comparator.comparing(HostChargerAnalyticsDTO.ChargerPerformanceDTO::getTotalBookings).reversed())
                .limit(5)
                .collect(Collectors.toList());
            
            List<HostChargerAnalyticsDTO.ChargerPerformanceDTO> topByRevenue = chargers.stream()
                .sorted(Comparator.comparing(HostChargerAnalyticsDTO.ChargerPerformanceDTO::getRevenue).reversed())
                .limit(5)
                .collect(Collectors.toList());
            
            HostChargerAnalyticsDTO result = HostChargerAnalyticsDTO.builder()
                .chargers(chargers)
                .topChargersByBookings(topByBookings)
                .topChargersByRevenue(topByRevenue)
                .build();
            
            log.info("=== SUCCESS: getChargerAnalytics completed successfully ===");
            return result;
            
        } catch (Exception e) {
            log.error("=== FATAL ERROR in getChargerAnalytics for hostId={} ===", hostId, e);
            throw new RuntimeException("Failed to calculate charger analytics: " + e.getMessage(), e);
        }
    }

    @Override
    @Cacheable(value = "hostBookings", key = "#hostId + '-' + #period")
    public HostBookingAnalyticsDTO getBookingAnalytics(Long hostId, AnalyticsPeriod period) {
        log.info("Calculating booking analytics for host {} with period {}", hostId, period);
        
        LocalDateTime startDate = getStartDate(period);
        
        // Get booking counts by status
        List<Object[]> statusData = bookingRepository.getBookingStatusDistribution(hostId, startDate);
        
        Long totalBookings = 0L;
        Long completedBookings = 0L;
        Long cancelledBookings = 0L;
        Long expiredBookings = 0L;
        Long activeBookings = 0L;
        
        List<DistributionData> distribution = new ArrayList<>();
        
        for (Object[] row : statusData) {
            String status = row[0].toString();
            Long count = (Long) row[1];
            totalBookings += count;
            
            switch (status) {
                case "COMPLETED" -> completedBookings = count;
                case "CANCELLED" -> cancelledBookings = count;
                case "EXPIRED" -> expiredBookings = count;
                case "ACTIVE", "CONFIRMED" -> activeBookings += count;
            }
        }
        
        // Calculate percentages for distribution
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
        
        // Expired bookings analysis
        HostBookingAnalyticsDTO.ExpiredBookingsAnalysis expiredAnalysis = getExpiredBookingsAnalysis(hostId);
        
        // Calculate rates
        BigDecimal completionRate = totalBookings > 0
            ? BigDecimal.valueOf(completedBookings).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalBookings), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        BigDecimal cancellationRate = totalBookings > 0
            ? BigDecimal.valueOf(cancelledBookings).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalBookings), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        BigDecimal expirationRate = totalBookings > 0
            ? BigDecimal.valueOf(expiredBookings).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalBookings), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        return HostBookingAnalyticsDTO.builder()
            .totalBookings(totalBookings)
            .completedBookings(completedBookings)
            .cancelledBookings(cancelledBookings)
            .expiredBookings(expiredBookings)
            .activeBookings(activeBookings)
            .bookingStatusDistribution(distribution)
            .expiredAnalysis(expiredAnalysis)
            .completionRate(completionRate)
            .cancellationRate(cancellationRate)
            .expirationRate(expirationRate)
            .build();
    }

    @Override
    @Cacheable(value = "hostUsers", key = "#hostId + '-' + #period")
    public HostUserAnalyticsDTO getUserAnalytics(Long hostId, AnalyticsPeriod period) {
        log.info("Calculating user analytics for host {} with period {}", hostId, period);
        
        LocalDateTime startDate = getStartDate(period);
        
        // Get active users count
        Long totalActiveUsers = bookingRepository.getActiveUserCountByHost(hostId, startDate);
        
        // Get top users
        List<Object[]> topUsersData = bookingRepository.getTopUsersByHost(hostId, startDate);
        List<HostUserAnalyticsDTO.TopUserDTO> topUsers = new ArrayList<>();
        
        Long newUsers = 0L;
        Long returningUsers = 0L;
        
        for (Object[] row : topUsersData) {
            Long userId = (Long) row[0];
            String firstName = (String) row[1];
            String lastName = (String) row[2];
            String email = (String) row[3];
            Long bookingCount = (Long) row[4];
            BigDecimal totalSpent = (BigDecimal) row[5];
            
            // Get favorite charger for this user
            List<Object[]> favoriteCharger = bookingRepository.getUserFavoriteChargerForHost(userId, hostId, startDate);
            Long favoriteChargerId = null;
            String favoriteChargerName = null;
            Long favoriteChargerVisits = 0L;
            
            if (!favoriteCharger.isEmpty()) {
                favoriteChargerId = (Long) favoriteCharger.get(0)[0];
                favoriteChargerName = (String) favoriteCharger.get(0)[1];
                favoriteChargerVisits = (Long) favoriteCharger.get(0)[2];
            }
            
            topUsers.add(HostUserAnalyticsDTO.TopUserDTO.builder()
                .userId(userId)
                .userName(firstName + " " + lastName)
                .userEmail(email)
                .totalBookings(bookingCount)
                .totalSpent(totalSpent)
                .favoriteChargerId(favoriteChargerId)
                .favoriteChargerName(favoriteChargerName)
                .favoriteChargerVisits(favoriteChargerVisits)
                .build());
        }
        
        // Get user-charger affinity
        List<Object[]> affinityData = bookingRepository.getUserChargerAffinity(hostId, startDate);
        List<HostUserAnalyticsDTO.UserChargerAffinityDTO> affinity = affinityData.stream()
            .map(row -> HostUserAnalyticsDTO.UserChargerAffinityDTO.builder()
                .userId((Long) row[0])
                .userName((String) row[1])
                .chargerId((Long) row[2])
                .chargerName((String) row[3])
                .bookingCount((Long) row[4])
                .totalSpent((BigDecimal) row[5])
                .build())
            .collect(Collectors.toList());
        
        // Calculate new vs returning (simplified - based on period)
        returningUsers = totalActiveUsers;
        
        return HostUserAnalyticsDTO.builder()
            .totalActiveUsers(totalActiveUsers)
            .newUsers(newUsers)
            .returningUsers(returningUsers)
            .topFrequentUsers(topUsers.stream().limit(10).collect(Collectors.toList()))
            .userChargerAffinity(affinity)
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

    private List<ChartDataPoint> getDailyRevenueData(Long hostId, LocalDateTime startDate) {
        List<Object[]> dailyData = bookingRepository.getDailyRevenueByHost(hostId, startDate);
        
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

    private List<HostRevenueAnalyticsDTO.ChargerRevenueDTO> getRevenueByChargerData(
            Long hostId, LocalDateTime startDate, BigDecimal totalRevenue) {
        
        List<Object[]> chargerRevenue = bookingRepository.getRevenueByCharger(hostId, startDate);
        
        return chargerRevenue.stream()
            .map(row -> {
                Long chargerId = (Long) row[0];
                String chargerName = (String) row[1];
                BigDecimal revenue = (BigDecimal) row[2];
                Long bookingCount = (Long) row[3];
                
                BigDecimal percentage = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? revenue.multiply(BigDecimal.valueOf(100))
                        .divide(totalRevenue, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                
                return HostRevenueAnalyticsDTO.ChargerRevenueDTO.builder()
                    .chargerId(chargerId)
                    .chargerName(chargerName)
                    .revenue(revenue)
                    .percentage(percentage)
                    .bookingCount(bookingCount)
                    .build();
            })
            .collect(Collectors.toList());
    }

    private HostRevenueAnalyticsDTO.PeriodBreakdown getPeriodBreakdown(Long hostId, String periodName, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        LocalDateTime previousStartDate = LocalDateTime.now().minusDays(days * 2);
        
        BigDecimal currentRevenue = bookingRepository.getTotalRevenueByHost(hostId, startDate);
        BigDecimal previousRevenue = bookingRepository.getTotalRevenueByHost(hostId, previousStartDate);
        Long bookings = bookingRepository.getTotalBookingsByHost(hostId, startDate);
        
        BigDecimal changePercentage = previousRevenue.compareTo(BigDecimal.ZERO) > 0
            ? currentRevenue.subtract(previousRevenue)
                .multiply(BigDecimal.valueOf(100))
                .divide(previousRevenue, 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        return HostRevenueAnalyticsDTO.PeriodBreakdown.builder()
            .period(periodName)
            .revenue(currentRevenue)
            .bookings(bookings)
            .changePercentage(changePercentage)
            .build();
    }

    private RatingDistribution getRatingDistributionForCharger(Long chargerId) {
        try {
            List<Object[]> ratingData = ratingRepository.getRatingDistributionByCharger(chargerId);
            RatingDistribution distribution = new RatingDistribution();
            
            if (ratingData == null || ratingData.isEmpty()) {
                log.debug("No rating distribution data for charger {}", chargerId);
                return distribution;
            }
            
            Long totalCount = ratingData.stream()
                .filter(row -> row != null && row.length > 1 && row[1] != null)
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();
            
            for (Object[] row : ratingData) {
                if (row == null || row.length < 2) continue;
                
                Integer stars = row[0] != null ? ((Number) row[0]).intValue() : null;
                Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                
                if (stars == null) continue;
                
                BigDecimal percentage = totalCount > 0
                    ? BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                
                distribution.setRating(stars, count, percentage);
            }
            
            return distribution;
        } catch (Exception e) {
            log.error("Error getting rating distribution for charger {}", chargerId, e);
            return new RatingDistribution();
        }
    }

    private List<HostChargerAnalyticsDTO.FrequentUserDTO> getFrequentUsersForCharger(
            Long hostId, Long chargerId, LocalDateTime startDate) {
        
        try {
            List<Object[]> userData = bookingRepository.getMostFrequentUsersByCharger(hostId, startDate);
            
            if (userData == null || userData.isEmpty()) {
                log.debug("No frequent users for charger {}", chargerId);
                return Collections.emptyList();
            }
            
            return userData.stream()
                .filter(row -> {
                    if (row == null || row.length < 5 || row[4] == null) {
                        return false;
                    }
                    try {
                        return row[4].equals(chargerId);
                    } catch (Exception e) {
                        log.warn("Error comparing charger ID in user data: {}", e.getMessage());
                        return false;
                    }
                })
                .map(row -> {
                    try {
                        String firstName = row[1] != null ? row[1].toString() : "";
                        String lastName = row[2] != null ? row[2].toString() : "";
                        String userName = (firstName + " " + lastName).trim();
                        if (userName.isEmpty()) {
                            userName = "Unknown User";
                        }
                        
                        Long userId = row[0] != null ? ((Number) row[0]).longValue() : null;
                        String email = row[3] != null ? row[3].toString() : "";
                        Long bookingCount = row[5] != null ? ((Number) row[5]).longValue() : 0L;
                        BigDecimal totalSpent = row[6] != null ? (BigDecimal) row[6] : BigDecimal.ZERO;
                        
                        return HostChargerAnalyticsDTO.FrequentUserDTO.builder()
                            .userId(userId)
                            .userName(userName)
                            .userEmail(email)
                            .bookingCount(bookingCount)
                            .totalSpent(totalSpent)
                            .build();
                    } catch (Exception e) {
                        log.error("Error mapping user data row: {}", Arrays.toString(row), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .limit(5)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Error getting frequent users for charger {}", chargerId, e);
            return Collections.emptyList();
        }
    }

    private HostBookingAnalyticsDTO.ExpiredBookingsAnalysis getExpiredBookingsAnalysis(Long hostId) {
        try {
            // This week
            LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
            Object[] weekData = bookingRepository.getExpiredBookingsAnalysis(hostId, weekStart);
            
            // This month
            LocalDateTime monthStart = LocalDateTime.now().minusDays(30);
            Object[] monthData = bookingRepository.getExpiredBookingsAnalysis(hostId, monthStart);
            
            // All time
            LocalDateTime allTimeStart = LocalDateTime.of(2000, 1, 1, 0, 0);
            Object[] allTimeData = bookingRepository.getExpiredBookingsAnalysis(hostId, allTimeStart);
            
            // Safely extract Long values from the result
            Long weekCount = weekData != null && weekData[0] != null ? ((Number) weekData[0]).longValue() : 0L;
            BigDecimal weekRevenue = weekData != null && weekData[1] != null ? (BigDecimal) weekData[1] : BigDecimal.ZERO;
            
            Long monthCount = monthData != null && monthData[0] != null ? ((Number) monthData[0]).longValue() : 0L;
            BigDecimal monthRevenue = monthData != null && monthData[1] != null ? (BigDecimal) monthData[1] : BigDecimal.ZERO;
            
            Long allTimeCount = allTimeData != null && allTimeData[0] != null ? ((Number) allTimeData[0]).longValue() : 0L;
            BigDecimal allTimeRevenue = allTimeData != null && allTimeData[1] != null ? (BigDecimal) allTimeData[1] : BigDecimal.ZERO;
            
            return HostBookingAnalyticsDTO.ExpiredBookingsAnalysis.builder()
                .totalExpiredCount(allTimeCount)
                .totalLostRevenue(allTimeRevenue)
                .thisWeek(HostBookingAnalyticsDTO.PeriodExpiredData.builder()
                    .period("This Week")
                    .expiredCount(weekCount)
                    .lostRevenue(weekRevenue)
                    .build())
                .thisMonth(HostBookingAnalyticsDTO.PeriodExpiredData.builder()
                    .period("This Month")
                    .expiredCount(monthCount)
                    .lostRevenue(monthRevenue)
                    .build())
                .allTime(HostBookingAnalyticsDTO.PeriodExpiredData.builder()
                    .period("All Time")
                    .expiredCount(allTimeCount)
                    .lostRevenue(allTimeRevenue)
                    .build())
                .build();
        } catch (Exception e) {
            log.error("Error getting expired bookings analysis for host {}: {}", hostId, e.getMessage(), e);
            // Return default values if there's an error
            return HostBookingAnalyticsDTO.ExpiredBookingsAnalysis.builder()
                .totalExpiredCount(0L)
                .totalLostRevenue(BigDecimal.ZERO)
                .thisWeek(HostBookingAnalyticsDTO.PeriodExpiredData.builder()
                    .period("This Week")
                    .expiredCount(0L)
                    .lostRevenue(BigDecimal.ZERO)
                    .build())
                .thisMonth(HostBookingAnalyticsDTO.PeriodExpiredData.builder()
                    .period("This Month")
                    .expiredCount(0L)
                    .lostRevenue(BigDecimal.ZERO)
                    .build())
                .allTime(HostBookingAnalyticsDTO.PeriodExpiredData.builder()
                    .period("All Time")
                    .expiredCount(0L)
                    .lostRevenue(BigDecimal.ZERO)
                    .build())
                .build();
        }
    }
}