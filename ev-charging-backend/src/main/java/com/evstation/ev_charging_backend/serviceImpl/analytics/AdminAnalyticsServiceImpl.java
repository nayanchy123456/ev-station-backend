package com.evstation.ev_charging_backend.serviceImpl.analytics;

import com.evstation.ev_charging_backend.dto.analytics.admin.*;
import com.evstation.ev_charging_backend.entity.Booking;
import com.evstation.ev_charging_backend.entity.Charger;
import com.evstation.ev_charging_backend.entity.Payment;
import com.evstation.ev_charging_backend.entity.Rating;
import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.enums.BookingStatus;
import com.evstation.ev_charging_backend.enums.PaymentStatus;
import com.evstation.ev_charging_backend.enums.Role;
import com.evstation.ev_charging_backend.repository.*;
import com.evstation.ev_charging_backend.service.analytics.AdminAnalyticsService;
import com.evstation.ev_charging_backend.service.analytics.AnalyticsValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ChargerRepository chargerRepository;
    private final PaymentRepository paymentRepository;
    private final RatingRepository ratingRepository;
    private final NotificationRepository notificationRepository;
    private final AnalyticsValidationService validationService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    public AdminOverviewDTO getOverviewAnalytics(LocalDate startDate, LocalDate endDate) {
        validationService.validateDateRange(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        Long totalUsers = userRepository.countByRole(Role.USER);
        Long totalHosts = userRepository.countByRole(Role.HOST);
        Long pendingHostApprovals = userRepository.countByRole(Role.PENDING_HOST);
        Long totalChargers = chargerRepository.count();
        Long activeBookings = bookingRepository.countByStatus(BookingStatus.ACTIVE);
        BigDecimal totalRevenue = calculateTotalRevenue(startDateTime, endDateTime);
        Double averageRating = calculateAverageRating();

        Long todaysBookings = bookingRepository.countByCreatedAtBetween(
            LocalDate.now().atStartOfDay(),
            LocalDate.now().atTime(23, 59, 59)
        );

        LocalDateTime weekStart = LocalDate.now().minusDays(7).atStartOfDay();
        BigDecimal thisWeekRevenue = calculateTotalRevenue(weekStart, LocalDateTime.now());

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Long newUsersLast30Days = userRepository.countByCreatedAtAfter(thirtyDaysAgo);

        Double completionRate = calculateCompletionRate(startDateTime, endDateTime);

        List<AdminOverviewDTO.TrendDataPoint> revenueTrend = calculateRevenueTrend(startDateTime, endDateTime);
        List<AdminOverviewDTO.TrendDataPoint> bookingTrend = calculateBookingTrend(startDateTime, endDateTime);
        List<AdminOverviewDTO.TrendDataPoint> userGrowthTrend = calculateUserGrowthTrend(startDateTime, endDateTime);

        return AdminOverviewDTO.builder()
                .totalUsers(totalUsers)
                .totalHosts(totalHosts)
                .pendingHostApprovals(pendingHostApprovals)
                .totalChargers(totalChargers)
                .activeBookings(activeBookings)
                .totalRevenue(totalRevenue)
                .averageRating(averageRating)
                .todaysBookings(todaysBookings)
                .thisWeekRevenue(thisWeekRevenue)
                .newUsersLast30Days(newUsersLast30Days)
                .completionRate(completionRate)
                .revenueTrend(revenueTrend)
                .bookingTrend(bookingTrend)
                .userGrowthTrend(userGrowthTrend)
                .build();
    }

    @Override
    public AdminUserAnalyticsDTO getUserAnalytics(LocalDate startDate, LocalDate endDate) {
        validationService.validateDateRange(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        Long totalRegisteredUsers = userRepository.count();
        Long activeUsers = userRepository.countUsersWithBookings();
        Double userGrowthRate = calculateUserGrowthRate(startDateTime, endDateTime);
        List<AdminUserAnalyticsDTO.UserRegistrationTrend> userRegistrationTimeline =
            calculateUserRegistrationTimeline(startDateTime, endDateTime);

        List<AdminUserAnalyticsDTO.TopUser> topActiveUsers = getTopActiveUsers(10);
        Double averageBookingsPerUser = calculateAverageBookingsPerUser();
        Double userRetentionRate = calculateUserRetentionRate();

        AdminUserAnalyticsDTO.UserSegmentation usersByRole = AdminUserAnalyticsDTO.UserSegmentation.builder()
                .totalUsers(totalRegisteredUsers)
                .hosts(userRepository.countByRole(Role.HOST))
                .regularUsers(userRepository.countByRole(Role.USER))
                .build();

        List<AdminUserAnalyticsDTO.ActivityHeatmap> activityHeatmap = calculateActivityHeatmap();

        return AdminUserAnalyticsDTO.builder()
                .totalRegisteredUsers(totalRegisteredUsers)
                .activeUsers(activeUsers)
                .userGrowthRate(userGrowthRate)
                .userRegistrationTimeline(userRegistrationTimeline)
                .topActiveUsers(topActiveUsers)
                .averageBookingsPerUser(averageBookingsPerUser)
                .userRetentionRate(userRetentionRate)
                .usersByRole(usersByRole)
                .activityHeatmap(activityHeatmap)
                .build();
    }

    // ========================================================================
    // Helper methods for Overview Analytics
    // ========================================================================

    private BigDecimal calculateTotalRevenue(LocalDateTime start, LocalDateTime end) {
        List<Payment> payments = paymentRepository.findByStatusAndCreatedAtBetween(
            PaymentStatus.SUCCESS, start, end
        );
        return payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Double calculateAverageRating() {
        return ratingRepository.findAverageRating().orElse(0.0);
    }

    private Double calculateCompletionRate(LocalDateTime start, LocalDateTime end) {
        Long totalBookings = bookingRepository.countByCreatedAtBetween(start, end);
        if (totalBookings == 0) return 0.0;

        Long completedBookings = bookingRepository.countByStatusAndCreatedAtBetween(
            BookingStatus.COMPLETED, start, end
        );

        return (completedBookings.doubleValue() / totalBookings.doubleValue()) * 100;
    }

    private List<AdminOverviewDTO.TrendDataPoint> calculateRevenueTrend(LocalDateTime start, LocalDateTime end) {
        List<AdminOverviewDTO.TrendDataPoint> trend = new ArrayList<>();
        LocalDate currentDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        while (!currentDate.isAfter(endDate)) {
            LocalDateTime dayStart = currentDate.atStartOfDay();
            LocalDateTime dayEnd = currentDate.atTime(23, 59, 59);

            BigDecimal dayRevenue = calculateTotalRevenue(dayStart, dayEnd);

            trend.add(AdminOverviewDTO.TrendDataPoint.builder()
                    .date(currentDate.format(DATE_FORMATTER))
                    .value(dayRevenue)
                    .build());

            currentDate = currentDate.plusDays(1);
        }

        return trend;
    }

    private List<AdminOverviewDTO.TrendDataPoint> calculateBookingTrend(LocalDateTime start, LocalDateTime end) {
        List<AdminOverviewDTO.TrendDataPoint> trend = new ArrayList<>();
        LocalDate currentDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        while (!currentDate.isAfter(endDate)) {
            LocalDateTime dayStart = currentDate.atStartOfDay();
            LocalDateTime dayEnd = currentDate.atTime(23, 59, 59);

            Long dayBookings = bookingRepository.countByCreatedAtBetween(dayStart, dayEnd);

            trend.add(AdminOverviewDTO.TrendDataPoint.builder()
                    .date(currentDate.format(DATE_FORMATTER))
                    .count(dayBookings)
                    .build());

            currentDate = currentDate.plusDays(1);
        }

        return trend;
    }

    private List<AdminOverviewDTO.TrendDataPoint> calculateUserGrowthTrend(LocalDateTime start, LocalDateTime end) {
        List<AdminOverviewDTO.TrendDataPoint> trend = new ArrayList<>();
        LocalDate currentDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        while (!currentDate.isAfter(endDate)) {
            LocalDateTime dayStart = currentDate.atStartOfDay();
            LocalDateTime dayEnd = currentDate.atTime(23, 59, 59);

            Long dayUsers = userRepository.countByCreatedAtBetween(dayStart, dayEnd);

            trend.add(AdminOverviewDTO.TrendDataPoint.builder()
                    .date(currentDate.format(DATE_FORMATTER))
                    .count(dayUsers)
                    .build());

            currentDate = currentDate.plusDays(1);
        }

        return trend;
    }

    // ========================================================================
    // Helper methods for User Analytics
    // ========================================================================

    private Double calculateUserGrowthRate(LocalDateTime start, LocalDateTime end) {
        Long previousPeriodUsers = userRepository.countByCreatedAtBefore(start);
        Long currentPeriodUsers = userRepository.countByCreatedAtBetween(start, end);

        if (previousPeriodUsers == 0) return 100.0;

        return ((currentPeriodUsers.doubleValue() / previousPeriodUsers.doubleValue()) * 100);
    }

    private List<AdminUserAnalyticsDTO.UserRegistrationTrend> calculateUserRegistrationTimeline(
            LocalDateTime start, LocalDateTime end) {
        List<AdminUserAnalyticsDTO.UserRegistrationTrend> timeline = new ArrayList<>();
        LocalDate currentDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        while (!currentDate.isAfter(endDate)) {
            LocalDateTime dayStart = currentDate.atStartOfDay();
            LocalDateTime dayEnd = currentDate.atTime(23, 59, 59);

            Long dayRegistrations = userRepository.countByCreatedAtBetween(dayStart, dayEnd);

            timeline.add(AdminUserAnalyticsDTO.UserRegistrationTrend.builder()
                    .date(currentDate.format(DATE_FORMATTER))
                    .count(dayRegistrations)
                    .build());

            currentDate = currentDate.plusDays(1);
        }

        return timeline;
    }

    private List<AdminUserAnalyticsDTO.TopUser> getTopActiveUsers(int limit) {
        List<Object[]> results = bookingRepository.findTopUsersByBookingCount(limit);

        return results.stream()
                .map(result -> {
                    User user = (User) result[0];
                    Long bookingCount = (Long) result[1];

                    return AdminUserAnalyticsDTO.TopUser.builder()
                            .userId(user.getUserId())
                            .name(user.getFirstName() + " " + user.getLastName())
                            .email(user.getEmail())
                            .bookingCount(bookingCount)
                            .lastActive(getLastActive(user.getUserId()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private LocalDateTime getLastActive(Long userId) {
        return bookingRepository.findTopByUserUserIdOrderByCreatedAtDesc(userId)
                .map(Booking::getCreatedAt)
                .orElse(null);
    }

    private Double calculateAverageBookingsPerUser() {
        Long totalUsers = userRepository.countByRole(Role.USER);
        if (totalUsers == 0) return 0.0;

        Long totalBookings = bookingRepository.count();
        return totalBookings.doubleValue() / totalUsers.doubleValue();
    }

    private Double calculateUserRetentionRate() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Long usersWithRecentActivity = userRepository.countUsersWithBookingsAfter(thirtyDaysAgo);
        Long totalUsers = userRepository.countByRole(Role.USER);

        if (totalUsers == 0) return 0.0;

        return (usersWithRecentActivity.doubleValue() / totalUsers.doubleValue()) * 100;
    }

    private List<AdminUserAnalyticsDTO.ActivityHeatmap> calculateActivityHeatmap() {
        List<Booking> bookings = bookingRepository.findAll();
        Map<String, Map<Integer, Long>> heatmapData = new HashMap<>();

        for (Booking booking : bookings) {
            String day = booking.getStartTime().getDayOfWeek().toString();
            Integer hour = booking.getStartTime().getHour();

            heatmapData
                .computeIfAbsent(day, k -> new HashMap<>())
                .merge(hour, 1L, Long::sum);
        }

        List<AdminUserAnalyticsDTO.ActivityHeatmap> heatmap = new ArrayList<>();
        for (Map.Entry<String, Map<Integer, Long>> dayEntry : heatmapData.entrySet()) {
            for (Map.Entry<Integer, Long> hourEntry : dayEntry.getValue().entrySet()) {
                heatmap.add(AdminUserAnalyticsDTO.ActivityHeatmap.builder()
                        .dayOfWeek(dayEntry.getKey())
                        .hour(hourEntry.getKey())
                        .activityCount(hourEntry.getValue())
                        .build());
            }
        }

        return heatmap;
    }

    // ========================================================================
    // Host Analytics
    // ========================================================================

    @Override
    public AdminHostAnalyticsDTO getHostAnalytics(LocalDate startDate, LocalDate endDate) {
        validationService.validateDateRange(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        Long totalHosts = userRepository.countByRole(Role.HOST);

        Long activeHosts = userRepository.findByRole(Role.HOST).stream()
                .filter(host -> !chargerRepository.findByHostUserId(host.getUserId()).isEmpty())
                .count();

        Long pendingApprovals = userRepository.countByRole(Role.PENDING_HOST);

        Long totalChargers = chargerRepository.count();
        Double avgChargersPerHost = totalHosts > 0 ?
            totalChargers.doubleValue() / totalHosts.doubleValue() : 0.0;

        BigDecimal totalHostRevenue = calculateTotalHostRevenue();
        Double avgHostRating = calculateAverageHostRating();

        Long totalApprovedHosts = totalHosts;
        Long pendingHostApprovals = pendingApprovals;
        Long rejectedHosts = 0L;
        Double averageChargersPerHost = avgChargersPerHost;

        List<AdminHostAnalyticsDTO.HostApprovalTimeline> hostApprovalTimeline =
            calculateHostApprovalTimeline(startDateTime, endDateTime);

        List<AdminHostAnalyticsDTO.TopHost> topPerformingHostsByRevenue = getTopHostsByRevenue(10);
        List<AdminHostAnalyticsDTO.TopHost> topRatedHosts = getTopRatedHosts(10);
        List<AdminHostAnalyticsDTO.TopHost> hostsWithMostBookings = getHostsWithMostBookings(10);

        Double averageApprovalTimeDays = 0.0;
        Double pendingToApprovedRatio = totalApprovedHosts > 0 ?
            pendingHostApprovals.doubleValue() / totalApprovedHosts.doubleValue() : 0.0;
        Double hostChurnRate = 0.0;

        return AdminHostAnalyticsDTO.builder()
                .totalHosts(totalHosts)
                .activeHosts(activeHosts)
                .pendingApprovals(pendingApprovals)
                .avgChargersPerHost(avgChargersPerHost)
                .totalHostRevenue(totalHostRevenue)
                .avgHostRating(avgHostRating)
                .totalApprovedHosts(totalApprovedHosts)
                .pendingHostApprovals(pendingHostApprovals)
                .rejectedHosts(rejectedHosts)
                .averageChargersPerHost(averageChargersPerHost)
                .hostApprovalTimeline(hostApprovalTimeline)
                .topPerformingHostsByRevenue(topPerformingHostsByRevenue)
                .topRatedHosts(topRatedHosts)
                .hostsWithMostBookings(hostsWithMostBookings)
                .averageApprovalTimeDays(averageApprovalTimeDays)
                .pendingToApprovedRatio(pendingToApprovedRatio)
                .hostChurnRate(hostChurnRate)
                .build();
    }

    private List<AdminHostAnalyticsDTO.HostApprovalTimeline> calculateHostApprovalTimeline(
            LocalDateTime start, LocalDateTime end) {
        List<AdminHostAnalyticsDTO.HostApprovalTimeline> timeline = new ArrayList<>();
        LocalDate currentDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        while (!currentDate.isAfter(endDate)) {
            LocalDateTime dayStart = currentDate.atStartOfDay();
            LocalDateTime dayEnd = currentDate.atTime(23, 59, 59);

            Long approved = userRepository.countByRoleAndCreatedAtBetween(Role.HOST, dayStart, dayEnd);
            Long pending = userRepository.countByRoleAndCreatedAtBetween(Role.PENDING_HOST, dayStart, dayEnd);

            timeline.add(AdminHostAnalyticsDTO.HostApprovalTimeline.builder()
                    .date(currentDate.format(DATE_FORMATTER))
                    .approved(approved)
                    .rejected(0L)
                    .pending(pending)
                    .build());

            currentDate = currentDate.plusDays(1);
        }

        return timeline;
    }

    private List<AdminHostAnalyticsDTO.TopHost> getTopHostsByRevenue(int limit) {
        List<User> hosts = userRepository.findByRole(Role.HOST);

        return hosts.stream()
                .map(host -> {
                    List<Charger> chargers = chargerRepository.findByHostUserId(host.getUserId());
                    BigDecimal totalRevenue = BigDecimal.ZERO;
                    Long bookingCount = 0L;

                    for (Charger charger : chargers) {
                        List<Booking> bookings = bookingRepository.findByChargerIdAndStatus(
                            charger.getId(), BookingStatus.COMPLETED);
                        bookingCount += bookings.size();

                        for (Booking booking : bookings) {
                            if (booking.getTotalPrice() != null) {
                                totalRevenue = totalRevenue.add(booking.getTotalPrice());
                            }
                        }
                    }

                    return AdminHostAnalyticsDTO.TopHost.builder()
                            .hostId(host.getUserId())
                            .name(host.getFirstName() + " " + host.getLastName())
                            .email(host.getEmail())
                            .totalRevenue(totalRevenue)
                            .bookingCount(bookingCount)
                            .averageRating(calculateHostAverageRating(host.getUserId()))
                            .chargerCount(chargers.size())
                            .joinedDate(host.getCreatedAt())
                            .build();
                })
                .sorted(Comparator.comparing(AdminHostAnalyticsDTO.TopHost::getTotalRevenue).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<AdminHostAnalyticsDTO.TopHost> getTopRatedHosts(int limit) {
        List<User> hosts = userRepository.findByRole(Role.HOST);

        return hosts.stream()
                .map(host -> {
                    List<Charger> chargers = chargerRepository.findByHostUserId(host.getUserId());
                    Double hostRating = calculateHostAverageRating(host.getUserId());
                    Long totalBookings = 0L;
                    BigDecimal totalRevenue = BigDecimal.ZERO;

                    for (Charger charger : chargers) {
                        List<Booking> bookings = bookingRepository.findByChargerIdAndStatus(
                            charger.getId(), BookingStatus.COMPLETED);
                        totalBookings += bookings.size();

                        for (Booking booking : bookings) {
                            if (booking.getTotalPrice() != null) {
                                totalRevenue = totalRevenue.add(booking.getTotalPrice());
                            }
                        }
                    }

                    return AdminHostAnalyticsDTO.TopHost.builder()
                            .hostId(host.getUserId())
                            .name(host.getFirstName() + " " + host.getLastName())
                            .email(host.getEmail())
                            .totalRevenue(totalRevenue)
                            .bookingCount(totalBookings)
                            .averageRating(hostRating)
                            .chargerCount(chargers.size())
                            .joinedDate(host.getCreatedAt())
                            .build();
                })
                .filter(host -> host.getAverageRating() != null && host.getAverageRating() > 0)
                .sorted(Comparator.comparing(AdminHostAnalyticsDTO.TopHost::getAverageRating).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<AdminHostAnalyticsDTO.TopHost> getHostsWithMostBookings(int limit) {
        List<User> hosts = userRepository.findByRole(Role.HOST);

        return hosts.stream()
                .map(host -> {
                    List<Charger> chargers = chargerRepository.findByHostUserId(host.getUserId());
                    Long totalBookings = 0L;
                    BigDecimal totalRevenue = BigDecimal.ZERO;

                    for (Charger charger : chargers) {
                        List<Booking> bookings = bookingRepository.findByChargerIdAndStatus(
                            charger.getId(), BookingStatus.COMPLETED);
                        totalBookings += bookings.size();

                        for (Booking booking : bookings) {
                            if (booking.getTotalPrice() != null) {
                                totalRevenue = totalRevenue.add(booking.getTotalPrice());
                            }
                        }
                    }

                    return AdminHostAnalyticsDTO.TopHost.builder()
                            .hostId(host.getUserId())
                            .name(host.getFirstName() + " " + host.getLastName())
                            .email(host.getEmail())
                            .totalRevenue(totalRevenue)
                            .bookingCount(totalBookings)
                            .averageRating(calculateHostAverageRating(host.getUserId()))
                            .chargerCount(chargers.size())
                            .joinedDate(host.getCreatedAt())
                            .build();
                })
                .sorted(Comparator.comparing(AdminHostAnalyticsDTO.TopHost::getBookingCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

private Double calculateHostAverageRating(Long hostId) {
    List<Charger> chargers = chargerRepository.findByHostUserId(hostId);
    if (chargers.isEmpty()) {
        return 0.0;
    }

    double totalRating = chargers.stream()
            .map(Charger::getRating)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .sum();
    
    long nonNullRatingsCount = chargers.stream()
            .map(Charger::getRating)
            .filter(Objects::nonNull)
            .count();

    return nonNullRatingsCount > 0 ? totalRating / nonNullRatingsCount : 0.0;
}

    // ========================================================================
    // Charger Analytics
    // ========================================================================

    @Override
    public AdminChargerAnalyticsDTO getChargerAnalytics(LocalDate startDate, LocalDate endDate) {
        validationService.validateDateRange(startDate, endDate);

        Long totalChargersRegistered = chargerRepository.count();
        List<AdminChargerAnalyticsDTO.BrandDistribution> chargersByBrand = getChargersByBrand();
        BigDecimal averagePricePerKwh = calculateAveragePricePerKwh();
        List<AdminChargerAnalyticsDTO.GeographicData> geographicDistribution = getGeographicDistribution();

        List<AdminChargerAnalyticsDTO.TopCharger> mostBookedChargers = getMostBookedChargers(10);
        List<AdminChargerAnalyticsDTO.TopCharger> highestRevenueChargers = getHighestRevenueChargers(10);
        List<AdminChargerAnalyticsDTO.TopCharger> topRatedChargers = getTopRatedChargers(10);
        List<AdminChargerAnalyticsDTO.TopCharger> underutilizedChargers = getUnderutilizedChargers(10);

        Double averageBookingRatePerCharger = calculateAverageBookingRatePerCharger();
        List<AdminChargerAnalyticsDTO.PeakTime> peakBookingTimes = getPeakBookingTimes();
        Double chargerAvailabilityRatio = 0.0;
        Double totalEnergyConsumedKwh = calculateTotalEnergyConsumed();

        return AdminChargerAnalyticsDTO.builder()
                .totalChargersRegistered(totalChargersRegistered)
                .chargersByBrand(chargersByBrand)
                .averagePricePerKwh(averagePricePerKwh)
                .geographicDistribution(geographicDistribution)
                .mostBookedChargers(mostBookedChargers)
                .highestRevenueChargers(highestRevenueChargers)
                .topRatedChargers(topRatedChargers)
                .underutilizedChargers(underutilizedChargers)
                .averageBookingRatePerCharger(averageBookingRatePerCharger)
                .peakBookingTimes(peakBookingTimes)
                .chargerAvailabilityRatio(chargerAvailabilityRatio)
                .totalEnergyConsumedKwh(totalEnergyConsumedKwh)
                .build();
    }

    private List<AdminChargerAnalyticsDTO.BrandDistribution> getChargersByBrand() {
        List<Charger> chargers = chargerRepository.findAll();
        Long total = (long) chargers.size();

        Map<String, Long> brandCount = chargers.stream()
                .collect(Collectors.groupingBy(Charger::getBrand, Collectors.counting()));

        return brandCount.entrySet().stream()
                .map(entry -> AdminChargerAnalyticsDTO.BrandDistribution.builder()
                        .brand(entry.getKey())
                        .count(entry.getValue())
                        .percentage(total > 0 ? (entry.getValue().doubleValue() / total.doubleValue()) * 100 : 0.0)
                        .build())
                .sorted(Comparator.comparing(AdminChargerAnalyticsDTO.BrandDistribution::getCount).reversed())
                .collect(Collectors.toList());
    }

    private BigDecimal calculateAveragePricePerKwh() {
        List<Charger> chargers = chargerRepository.findAll();
        if (chargers.isEmpty()) return BigDecimal.ZERO;

        BigDecimal totalPrice = chargers.stream()
                .map(Charger::getPricePerKwh)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalPrice.divide(BigDecimal.valueOf(chargers.size()), 2, RoundingMode.HALF_UP);
    }

    private List<AdminChargerAnalyticsDTO.GeographicData> getGeographicDistribution() {
        List<Charger> chargers = chargerRepository.findAll();

        Map<String, List<Charger>> chargersByLocation = chargers.stream()
                .collect(Collectors.groupingBy(Charger::getLocation));

        return chargersByLocation.entrySet().stream()
                .map(entry -> {
                    String location = entry.getKey();
                    List<Charger> locationChargers = entry.getValue();

                    Long bookingCount = locationChargers.stream()
                            .mapToLong(charger -> bookingRepository.countByChargerId(charger.getId()))
                            .sum();

                    BigDecimal revenue = locationChargers.stream()
                            .flatMap(charger -> bookingRepository.findByChargerIdAndStatus(
                                charger.getId(), BookingStatus.COMPLETED).stream())
                            .map(Booking::getTotalPrice)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return AdminChargerAnalyticsDTO.GeographicData.builder()
                            .location(location)
                            .chargerCount((long) locationChargers.size())
                            .bookingCount(bookingCount)
                            .revenue(revenue)
                            .build();
                })
                .sorted(Comparator.comparing(AdminChargerAnalyticsDTO.GeographicData::getBookingCount).reversed())
                .collect(Collectors.toList());
    }

    private List<AdminChargerAnalyticsDTO.TopCharger> getMostBookedChargers(int limit) {
    List<Charger> chargers = chargerRepository.findAll();

    return chargers.stream()
            .map(charger -> {
                Long bookingCount = bookingRepository.countByChargerId(charger.getId());
                BigDecimal revenue = bookingRepository.findByChargerIdAndStatus(
                        charger.getId(), BookingStatus.COMPLETED).stream()
                        .map(Booking::getTotalPrice)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                String hostName = charger.getHost() != null ?
                    charger.getHost().getFirstName() + " " + charger.getHost().getLastName() : "N/A";

                return AdminChargerAnalyticsDTO.TopCharger.builder()
                        .chargerId(charger.getId())
                        .name(charger.getName())
                        .brand(charger.getBrand())
                        .location(charger.getLocation())
                        .bookingCount(bookingCount)
                        .totalRevenue(revenue)
                        .averageRating(charger.getRating() != null ? charger.getRating() : 0.0) // FIXED
                        .pricePerKwh(charger.getPricePerKwh())
                        .hostName(hostName)
                        .build();
            })
            .sorted(Comparator.comparing(AdminChargerAnalyticsDTO.TopCharger::getBookingCount).reversed())
            .limit(limit)
            .collect(Collectors.toList());
}

    private List<AdminChargerAnalyticsDTO.TopCharger> getHighestRevenueChargers(int limit) {
    List<Charger> chargers = chargerRepository.findAll();

    return chargers.stream()
            .map(charger -> {
                Long bookingCount = bookingRepository.countByChargerId(charger.getId());
                BigDecimal revenue = bookingRepository.findByChargerIdAndStatus(
                        charger.getId(), BookingStatus.COMPLETED).stream()
                        .map(Booking::getTotalPrice)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                String hostName = charger.getHost() != null ?
                    charger.getHost().getFirstName() + " " + charger.getHost().getLastName() : "N/A";

                return AdminChargerAnalyticsDTO.TopCharger.builder()
                        .chargerId(charger.getId())
                        .name(charger.getName())
                        .brand(charger.getBrand())
                        .location(charger.getLocation())
                        .bookingCount(bookingCount)
                        .totalRevenue(revenue)
                        .averageRating(charger.getRating() != null ? charger.getRating() : 0.0) // FIXED
                        .pricePerKwh(charger.getPricePerKwh())
                        .hostName(hostName)
                        .build();
            })
            .sorted(Comparator.comparing(AdminChargerAnalyticsDTO.TopCharger::getTotalRevenue).reversed())
            .limit(limit)
            .collect(Collectors.toList());
}

   private List<AdminChargerAnalyticsDTO.TopCharger> getTopRatedChargers(int limit) {
    List<Charger> chargers = chargerRepository.findAll();

    return chargers.stream()
            .filter(charger -> charger.getRating() != null && charger.getRating() > 0) // FIXED
            .map(charger -> {
                Long bookingCount = bookingRepository.countByChargerId(charger.getId());
                BigDecimal revenue = bookingRepository.findByChargerIdAndStatus(
                        charger.getId(), BookingStatus.COMPLETED).stream()
                        .map(Booking::getTotalPrice)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                String hostName = charger.getHost() != null ?
                    charger.getHost().getFirstName() + " " + charger.getHost().getLastName() : "N/A";

                return AdminChargerAnalyticsDTO.TopCharger.builder()
                        .chargerId(charger.getId())
                        .name(charger.getName())
                        .brand(charger.getBrand())
                        .location(charger.getLocation())
                        .bookingCount(bookingCount)
                        .totalRevenue(revenue)
                        .averageRating(charger.getRating() != null ? charger.getRating() : 0.0) // FIXED
                        .pricePerKwh(charger.getPricePerKwh())
                        .hostName(hostName)
                        .build();
            })
            .sorted(Comparator.comparing(AdminChargerAnalyticsDTO.TopCharger::getAverageRating).reversed())
            .limit(limit)
            .collect(Collectors.toList());
}
    private List<AdminChargerAnalyticsDTO.TopCharger> getUnderutilizedChargers(int limit) {
        List<Charger> chargers = chargerRepository.findAll();

        return chargers.stream()
                .map(charger -> {
                    Long bookingCount = bookingRepository.countByChargerId(charger.getId());
                    BigDecimal revenue = bookingRepository.findByChargerIdAndStatus(
                            charger.getId(), BookingStatus.COMPLETED).stream()
                            .map(Booking::getTotalPrice)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    String hostName = charger.getHost() != null ?
                        charger.getHost().getFirstName() + " " + charger.getHost().getLastName() : "N/A";

                    return AdminChargerAnalyticsDTO.TopCharger.builder()
                            .chargerId(charger.getId())
                            .name(charger.getName())
                            .brand(charger.getBrand())
                            .location(charger.getLocation())
                            .bookingCount(bookingCount)
                            .totalRevenue(revenue)
                            .averageRating(charger.getRating())
                            .pricePerKwh(charger.getPricePerKwh())
                            .hostName(hostName)
                            .build();
                })
                .sorted(Comparator.comparing(AdminChargerAnalyticsDTO.TopCharger::getBookingCount))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private Double calculateAverageBookingRatePerCharger() {
        Long totalChargers = chargerRepository.count();
        if (totalChargers == 0) return 0.0;

        Long totalBookings = bookingRepository.count();
        return totalBookings.doubleValue() / totalChargers.doubleValue();
    }

    private List<AdminChargerAnalyticsDTO.PeakTime> getPeakBookingTimes() {
        List<Booking> bookings = bookingRepository.findAll();

        Map<Integer, Map<String, Long>> timeData = new HashMap<>();

        for (Booking booking : bookings) {
            Integer hour = booking.getStartTime().getHour();
            String day = booking.getStartTime().getDayOfWeek().toString();

            timeData
                .computeIfAbsent(hour, k -> new HashMap<>())
                .merge(day, 1L, Long::sum);
        }

        List<AdminChargerAnalyticsDTO.PeakTime> peakTimes = new ArrayList<>();
        for (Map.Entry<Integer, Map<String, Long>> hourEntry : timeData.entrySet()) {
            for (Map.Entry<String, Long> dayEntry : hourEntry.getValue().entrySet()) {
                peakTimes.add(AdminChargerAnalyticsDTO.PeakTime.builder()
                        .hour(hourEntry.getKey())
                        .dayOfWeek(dayEntry.getKey())
                        .bookingCount(dayEntry.getValue())
                        .build());
            }
        }

        return peakTimes.stream()
                .sorted(Comparator.comparing(AdminChargerAnalyticsDTO.PeakTime::getBookingCount).reversed())
                .limit(20)
                .collect(Collectors.toList());
    }

    private Double calculateTotalEnergyConsumed() {
        List<Booking> completedBookings = bookingRepository.findByStatus(BookingStatus.COMPLETED);

        return completedBookings.stream()
                .map(Booking::getTotalEnergyKwh)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    // ========================================================================
    // Booking Analytics
    // ========================================================================

    @Override
    public AdminBookingAnalyticsDTO getBookingAnalytics(LocalDate startDate, LocalDate endDate) {
        validationService.validateDateRange(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        Long totalBookings = bookingRepository.countByCreatedAtBetween(startDateTime, endDateTime);
        List<AdminBookingAnalyticsDTO.StatusDistribution> bookingsByStatus = getBookingsByStatus(startDateTime, endDateTime);
        Double bookingCompletionRate = calculateBookingCompletionRate(startDateTime, endDateTime);
        Double averageBookingDurationHours = calculateAverageBookingDuration();
        Double cancellationRate = calculateCancellationRate(startDateTime, endDateTime);

        List<AdminBookingAnalyticsDTO.BookingTrend> dailyBookingTrend =
            calculateDailyBookingTrend(startDateTime, endDateTime);
        List<AdminBookingAnalyticsDTO.BookingTrend> weeklyBookingComparison =
            calculateWeeklyBookingComparison(startDateTime, endDateTime);
        List<AdminBookingAnalyticsDTO.BookingTrend> monthlyBookingComparison =
            calculateMonthlyBookingComparison(startDateTime, endDateTime);
        List<AdminBookingAnalyticsDTO.PeakHour> peakBookingHours = getPeakBookingHours();
        List<AdminBookingAnalyticsDTO.DayFrequency> bookingFrequencyByDay = getBookingFrequencyByDay();

        Double averageLeadTimeHours = calculateAverageLeadTime();
        List<AdminBookingAnalyticsDTO.DurationDistribution> popularBookingDurations = getPopularBookingDurations();
        Double reservationExpiryRate = 0.0;
        Double repeatBookingRate = calculateRepeatBookingRate();

        return AdminBookingAnalyticsDTO.builder()
                .totalBookings(totalBookings)
                .bookingsByStatus(bookingsByStatus)
                .bookingCompletionRate(bookingCompletionRate)
                .averageBookingDurationHours(averageBookingDurationHours)
                .cancellationRate(cancellationRate)
                .dailyBookingTrend(dailyBookingTrend)
                .weeklyBookingComparison(weeklyBookingComparison)
                .monthlyBookingComparison(monthlyBookingComparison)
                .peakBookingHours(peakBookingHours)
                .bookingFrequencyByDay(bookingFrequencyByDay)
                .averageLeadTimeHours(averageLeadTimeHours)
                .popularBookingDurations(popularBookingDurations)
                .reservationExpiryRate(reservationExpiryRate)
                .repeatBookingRate(repeatBookingRate)
                .build();
    }

    private List<AdminBookingAnalyticsDTO.StatusDistribution> getBookingsByStatus(
            LocalDateTime start, LocalDateTime end) {
        Long total = bookingRepository.countByCreatedAtBetween(start, end);

        return Arrays.stream(BookingStatus.values())
                .map(status -> {
                    Long count = bookingRepository.countByStatusAndCreatedAtBetween(status, start, end);
                    Double percentage = total > 0 ? (count.doubleValue() / total.doubleValue()) * 100 : 0.0;

                    return AdminBookingAnalyticsDTO.StatusDistribution.builder()
                            .status(status.name())
                            .count(count)
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private Double calculateBookingCompletionRate(LocalDateTime start, LocalDateTime end) {
        Long total = bookingRepository.countByCreatedAtBetween(start, end);
        if (total == 0) return 0.0;

        Long completed = bookingRepository.countByStatusAndCreatedAtBetween(BookingStatus.COMPLETED, start, end);
        return (completed.doubleValue() / total.doubleValue()) * 100;
    }

    private Double calculateAverageBookingDuration() {
        List<Booking> bookings = bookingRepository.findAll();
        if (bookings.isEmpty()) return 0.0;

        double totalHours = bookings.stream()
                .mapToDouble(booking -> ChronoUnit.HOURS.between(
                    booking.getStartTime(), booking.getEndTime()))
                .sum();

        return totalHours / bookings.size();
    }

    private Double calculateCancellationRate(LocalDateTime start, LocalDateTime end) {
        Long total = bookingRepository.countByCreatedAtBetween(start, end);
        if (total == 0) return 0.0;

        Long cancelled = bookingRepository.countByStatusAndCreatedAtBetween(BookingStatus.CANCELLED, start, end);
        return (cancelled.doubleValue() / total.doubleValue()) * 100;
    }

    private List<AdminBookingAnalyticsDTO.BookingTrend> calculateDailyBookingTrend(
            LocalDateTime start, LocalDateTime end) {
        List<AdminBookingAnalyticsDTO.BookingTrend> trend = new ArrayList<>();
        LocalDate currentDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        while (!currentDate.isAfter(endDate)) {
            LocalDateTime dayStart = currentDate.atStartOfDay();
            LocalDateTime dayEnd = currentDate.atTime(23, 59, 59);

            Long count = bookingRepository.countByCreatedAtBetween(dayStart, dayEnd);

            trend.add(AdminBookingAnalyticsDTO.BookingTrend.builder()
                    .date(currentDate.format(DATE_FORMATTER))
                    .count(count)
                    .period("daily")
                    .build());

            currentDate = currentDate.plusDays(1);
        }

        return trend;
    }

    private List<AdminBookingAnalyticsDTO.BookingTrend> calculateWeeklyBookingComparison(
            LocalDateTime start, LocalDateTime end) {
        List<AdminBookingAnalyticsDTO.BookingTrend> trend = new ArrayList<>();
        LocalDate currentDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        while (!currentDate.isAfter(endDate)) {
            LocalDate weekEnd = currentDate.plusDays(6);
            if (weekEnd.isAfter(endDate)) weekEnd = endDate;

            Long count = bookingRepository.countByCreatedAtBetween(
                currentDate.atStartOfDay(),
                weekEnd.atTime(23, 59, 59)
            );

            trend.add(AdminBookingAnalyticsDTO.BookingTrend.builder()
                    .date("Week of " + currentDate.format(DATE_FORMATTER))
                    .count(count)
                    .period("weekly")
                    .build());

            currentDate = currentDate.plusWeeks(1);
        }

        return trend;
    }

    private List<AdminBookingAnalyticsDTO.BookingTrend> calculateMonthlyBookingComparison(
            LocalDateTime start, LocalDateTime end) {
        List<AdminBookingAnalyticsDTO.BookingTrend> trend = new ArrayList<>();
        LocalDate currentDate = start.toLocalDate().withDayOfMonth(1);
        LocalDate endDate = end.toLocalDate();

        while (!currentDate.isAfter(endDate)) {
            LocalDate monthEnd = currentDate.plusMonths(1).minusDays(1);
            if (monthEnd.isAfter(endDate)) monthEnd = endDate;

            Long count = bookingRepository.countByCreatedAtBetween(
                currentDate.atStartOfDay(),
                monthEnd.atTime(23, 59, 59)
            );

            trend.add(AdminBookingAnalyticsDTO.BookingTrend.builder()
                    .date(currentDate.format(MONTH_FORMATTER))
                    .count(count)
                    .period("monthly")
                    .build());

            currentDate = currentDate.plusMonths(1);
        }

        return trend;
    }

    private List<AdminBookingAnalyticsDTO.PeakHour> getPeakBookingHours() {
        List<Booking> bookings = bookingRepository.findAll();

        Map<Integer, Long> hourCounts = bookings.stream()
                .collect(Collectors.groupingBy(
                    booking -> booking.getStartTime().getHour(),
                    Collectors.counting()
                ));

        return hourCounts.entrySet().stream()
                .map(entry -> AdminBookingAnalyticsDTO.PeakHour.builder()
                        .hour(entry.getKey())
                        .bookingCount(entry.getValue())
                        .build())
                .sorted(Comparator.comparing(AdminBookingAnalyticsDTO.PeakHour::getBookingCount).reversed())
                .collect(Collectors.toList());
    }

    private List<AdminBookingAnalyticsDTO.DayFrequency> getBookingFrequencyByDay() {
        List<Booking> bookings = bookingRepository.findAll();

        Map<String, Long> dayCounts = bookings.stream()
                .collect(Collectors.groupingBy(
                    booking -> booking.getStartTime().getDayOfWeek().toString(),
                    Collectors.counting()
                ));

        return dayCounts.entrySet().stream()
                .map(entry -> AdminBookingAnalyticsDTO.DayFrequency.builder()
                        .dayOfWeek(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .sorted(Comparator.comparing(AdminBookingAnalyticsDTO.DayFrequency::getCount).reversed())
                .collect(Collectors.toList());
    }

    private Double calculateAverageLeadTime() {
        List<Booking> bookings = bookingRepository.findAll();
        if (bookings.isEmpty()) return 0.0;

        double totalLeadTimeHours = bookings.stream()
                .mapToDouble(booking -> ChronoUnit.HOURS.between(
                    booking.getCreatedAt(), booking.getStartTime()))
                .sum();

        return totalLeadTimeHours / bookings.size();
    }

    private List<AdminBookingAnalyticsDTO.DurationDistribution> getPopularBookingDurations() {
        List<Booking> bookings = bookingRepository.findAll();

        Map<String, Long> durationCounts = bookings.stream()
                .collect(Collectors.groupingBy(
                    booking -> {
                        long hours = ChronoUnit.HOURS.between(booking.getStartTime(), booking.getEndTime());
                        if (hours <= 1) return "0-1 hours";
                        else if (hours <= 2) return "1-2 hours";
                        else if (hours <= 4) return "2-4 hours";
                        else if (hours <= 8) return "4-8 hours";
                        else return "8+ hours";
                    },
                    Collectors.counting()
                ));

        return durationCounts.entrySet().stream()
                .map(entry -> AdminBookingAnalyticsDTO.DurationDistribution.builder()
                        .durationRange(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .sorted(Comparator.comparing(AdminBookingAnalyticsDTO.DurationDistribution::getCount).reversed())
                .collect(Collectors.toList());
    }

    private Double calculateRepeatBookingRate() {
        List<Object[]> userBookingCounts = bookingRepository.findUserBookingCounts();

        long usersWithMultipleBookings = userBookingCounts.stream()
                .filter(result -> (Long) result[1] > 1)
                .count();

        long totalUsers = userBookingCounts.size();

        if (totalUsers == 0) return 0.0;

        return (usersWithMultipleBookings / (double) totalUsers) * 100;
    }

    // ========================================================================
    // Revenue Analytics
    // ========================================================================

    @Override
    public AdminRevenueAnalyticsDTO getRevenueAnalytics(LocalDate startDate, LocalDate endDate) {
        validationService.validateDateRange(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        BigDecimal totalRevenue = calculateTotalRevenue(startDateTime, endDateTime);
        Double revenueGrowthRate = calculateRevenueGrowthRate(startDateTime, endDateTime);
        BigDecimal averageRevenuePerBooking = calculateAverageRevenuePerBooking(startDateTime, endDateTime);
        List<AdminRevenueAnalyticsDTO.MonthlyRevenue> revenueByMonth =
            calculateRevenueByMonth(startDateTime, endDateTime);

        List<AdminRevenueAnalyticsDTO.RevenueByCharger> revenueByCharger = getRevenueByCharger();
        List<AdminRevenueAnalyticsDTO.RevenueByHost> revenueByHost = getRevenueByHost();
        List<AdminRevenueAnalyticsDTO.RevenueByLocation> revenueByLocation = getRevenueByLocation();

        Double totalEnergyConsumedKwh = calculateTotalEnergyConsumed();
        BigDecimal averageTransactionValue = calculateAverageTransactionValue();
        BigDecimal revenuePerUser = calculateRevenuePerUser(totalRevenue);
        BigDecimal revenuePerCharger = calculateRevenuePerCharger(totalRevenue);

        Double paymentSuccessRate = calculatePaymentSuccessRate();
        List<AdminRevenueAnalyticsDTO.PaymentMethodDistribution> paymentMethodDistribution =
            getPaymentMethodDistribution();
        Long failedPaymentCount = paymentRepository.countByStatus(PaymentStatus.FAILED);
        AdminRevenueAnalyticsDTO.RefundStatistics refundStatistics = getRefundStatistics();

        return AdminRevenueAnalyticsDTO.builder()
                .totalRevenue(totalRevenue)
                .revenueGrowthRate(revenueGrowthRate)
                .averageRevenuePerBooking(averageRevenuePerBooking)
                .revenueByMonth(revenueByMonth)
                .revenueByCharger(revenueByCharger)
                .revenueByHost(revenueByHost)
                .revenueByLocation(revenueByLocation)
                .totalEnergyConsumedKwh(totalEnergyConsumedKwh)
                .averageTransactionValue(averageTransactionValue)
                .revenuePerUser(revenuePerUser)
                .revenuePerCharger(revenuePerCharger)
                .paymentSuccessRate(paymentSuccessRate)
                .paymentMethodDistribution(paymentMethodDistribution)
                .failedPaymentCount(failedPaymentCount)
                .refundStatistics(refundStatistics)
                .build();
    }

    private Double calculateRevenueGrowthRate(LocalDateTime start, LocalDateTime end) {
        long daysBetween = ChronoUnit.DAYS.between(start, end);
        LocalDateTime previousStart = start.minusDays(daysBetween);

        BigDecimal previousRevenue = calculateTotalRevenue(previousStart, start);
        BigDecimal currentRevenue = calculateTotalRevenue(start, end);

        if (previousRevenue.compareTo(BigDecimal.ZERO) == 0) return 100.0;

        BigDecimal growth = currentRevenue.subtract(previousRevenue)
                .divide(previousRevenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return growth.doubleValue();
    }

    private BigDecimal calculateAverageRevenuePerBooking(LocalDateTime start, LocalDateTime end) {
        List<Booking> bookings = bookingRepository.findByCreatedAtBetween(start, end);
        if (bookings.isEmpty()) return BigDecimal.ZERO;

        BigDecimal totalRevenue = bookings.stream()
                .map(Booking::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalRevenue.divide(BigDecimal.valueOf(bookings.size()), 2, RoundingMode.HALF_UP);
    }

    private List<AdminRevenueAnalyticsDTO.MonthlyRevenue> calculateRevenueByMonth(
            LocalDateTime start, LocalDateTime end) {
        List<AdminRevenueAnalyticsDTO.MonthlyRevenue> monthlyData = new ArrayList<>();
        LocalDate currentDate = start.toLocalDate().withDayOfMonth(1);
        LocalDate endDate = end.toLocalDate();

        while (!currentDate.isAfter(endDate)) {
            LocalDate monthEnd = currentDate.plusMonths(1).minusDays(1);
            if (monthEnd.isAfter(endDate)) monthEnd = endDate;

            LocalDateTime monthStart = currentDate.atStartOfDay();
            LocalDateTime monthEndTime = monthEnd.atTime(23, 59, 59);

            BigDecimal revenue = calculateTotalRevenue(monthStart, monthEndTime);
            Long bookingCount = bookingRepository.countByCreatedAtBetween(monthStart, monthEndTime);

            monthlyData.add(AdminRevenueAnalyticsDTO.MonthlyRevenue.builder()
                    .month(currentDate.format(MONTH_FORMATTER))
                    .revenue(revenue)
                    .bookingCount(bookingCount)
                    .build());

            currentDate = currentDate.plusMonths(1);
        }

        return monthlyData;
    }

    private List<AdminRevenueAnalyticsDTO.RevenueByCharger> getRevenueByCharger() {
        List<Charger> chargers = chargerRepository.findAll();

        return chargers.stream()
                .map(charger -> {
                    List<Booking> bookings = bookingRepository.findByChargerIdAndStatus(
                        charger.getId(), BookingStatus.COMPLETED);

                    BigDecimal revenue = bookings.stream()
                            .map(Booking::getTotalPrice)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return AdminRevenueAnalyticsDTO.RevenueByCharger.builder()
                            .chargerId(charger.getId())
                            .chargerName(charger.getName())
                            .revenue(revenue)
                            .bookingCount((long) bookings.size())
                            .build();
                })
                .sorted(Comparator.comparing(AdminRevenueAnalyticsDTO.RevenueByCharger::getRevenue).reversed())
                .limit(20)
                .collect(Collectors.toList());
    }

    private List<AdminRevenueAnalyticsDTO.RevenueByHost> getRevenueByHost() {
        List<User> hosts = userRepository.findByRole(Role.HOST);

        return hosts.stream()
                .map(host -> {
                    List<Charger> chargers = chargerRepository.findByHostUserId(host.getUserId());

                    BigDecimal totalRevenue = BigDecimal.ZERO;
                    Long totalBookings = 0L;

                    for (Charger charger : chargers) {
                        List<Booking> bookings = bookingRepository.findByChargerIdAndStatus(
                            charger.getId(), BookingStatus.COMPLETED);
                        totalBookings += bookings.size();

                        BigDecimal chargerRevenue = bookings.stream()
                                .map(Booking::getTotalPrice)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        totalRevenue = totalRevenue.add(chargerRevenue);
                    }

                    return AdminRevenueAnalyticsDTO.RevenueByHost.builder()
                            .hostId(host.getUserId())
                            .hostName(host.getFirstName() + " " + host.getLastName())
                            .revenue(totalRevenue)
                            .bookingCount(totalBookings)
                            .chargerCount(chargers.size())
                            .build();
                })
                .sorted(Comparator.comparing(AdminRevenueAnalyticsDTO.RevenueByHost::getRevenue).reversed())
                .limit(20)
                .collect(Collectors.toList());
    }

    private List<AdminRevenueAnalyticsDTO.RevenueByLocation> getRevenueByLocation() {
        List<Charger> chargers = chargerRepository.findAll();

        Map<String, List<Charger>> chargersByLocation = chargers.stream()
                .collect(Collectors.groupingBy(Charger::getLocation));

        return chargersByLocation.entrySet().stream()
                .map(entry -> {
                    String location = entry.getKey();
                    List<Charger> locationChargers = entry.getValue();

                    BigDecimal totalRevenue = BigDecimal.ZERO;
                    Long totalBookings = 0L;

                    for (Charger charger : locationChargers) {
                        List<Booking> bookings = bookingRepository.findByChargerIdAndStatus(
                            charger.getId(), BookingStatus.COMPLETED);
                        totalBookings += bookings.size();

                        BigDecimal chargerRevenue = bookings.stream()
                                .map(Booking::getTotalPrice)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        totalRevenue = totalRevenue.add(chargerRevenue);
                    }

                    return AdminRevenueAnalyticsDTO.RevenueByLocation.builder()
                            .location(location)
                            .revenue(totalRevenue)
                            .bookingCount(totalBookings)
                            .chargerCount(locationChargers.size())
                            .build();
                })
                .sorted(Comparator.comparing(AdminRevenueAnalyticsDTO.RevenueByLocation::getRevenue).reversed())
                .collect(Collectors.toList());
    }

    private BigDecimal calculateAverageTransactionValue() {
        List<Payment> payments = paymentRepository.findByStatus(PaymentStatus.SUCCESS);
        if (payments.isEmpty()) return BigDecimal.ZERO;

        BigDecimal totalAmount = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalAmount.divide(BigDecimal.valueOf(payments.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRevenuePerUser(BigDecimal totalRevenue) {
        Long totalUsers = userRepository.countByRole(Role.USER);
        if (totalUsers == 0) return BigDecimal.ZERO;

        return totalRevenue.divide(BigDecimal.valueOf(totalUsers), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRevenuePerCharger(BigDecimal totalRevenue) {
        Long totalChargers = chargerRepository.count();
        if (totalChargers == 0) return BigDecimal.ZERO;

        return totalRevenue.divide(BigDecimal.valueOf(totalChargers), 2, RoundingMode.HALF_UP);
    }

    private Double calculatePaymentSuccessRate() {
        Long totalPayments = paymentRepository.count();
        if (totalPayments == 0) return 0.0;

        Long successfulPayments = paymentRepository.countByStatus(PaymentStatus.SUCCESS);
        return (successfulPayments.doubleValue() / totalPayments.doubleValue()) * 100;
    }

    private List<AdminRevenueAnalyticsDTO.PaymentMethodDistribution> getPaymentMethodDistribution() {
        List<Payment> payments = paymentRepository.findAll();
        Long total = (long) payments.size();

        Map<String, List<Payment>> paymentsByMethod = payments.stream()
                .filter(p -> p.getPaymentMethod() != null)
                .collect(Collectors.groupingBy(Payment::getPaymentMethod));

        return paymentsByMethod.entrySet().stream()
                .map(entry -> {
                    List<Payment> methodPayments = entry.getValue();
                    BigDecimal totalAmount = methodPayments.stream()
                            .map(Payment::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return AdminRevenueAnalyticsDTO.PaymentMethodDistribution.builder()
                            .paymentMethod(entry.getKey())
                            .count((long) methodPayments.size())
                            .totalAmount(totalAmount)
                            .percentage(total > 0 ? (methodPayments.size() / (double) total) * 100 : 0.0)
                            .build();
                })
                .sorted(Comparator.comparing(AdminRevenueAnalyticsDTO.PaymentMethodDistribution::getCount).reversed())
                .collect(Collectors.toList());
    }

    private AdminRevenueAnalyticsDTO.RefundStatistics getRefundStatistics() {
        List<Payment> refundedPayments = paymentRepository.findByStatus(PaymentStatus.REFUNDED);

        Long totalRefunds = (long) refundedPayments.size();
        BigDecimal totalRefundAmount = refundedPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long totalPayments = paymentRepository.count();
        Double refundRate = totalPayments > 0 ?
            (totalRefunds.doubleValue() / totalPayments.doubleValue()) * 100 : 0.0;

        return AdminRevenueAnalyticsDTO.RefundStatistics.builder()
                .totalRefunds(totalRefunds)
                .totalRefundAmount(totalRefundAmount)
                .refundRate(refundRate)
                .build();
    }

    // ========================================================================
    // Rating Analytics
    // ========================================================================

    @Override
    public AdminRatingAnalyticsDTO getRatingAnalytics(LocalDate startDate, LocalDate endDate) {
    validationService.validateDateRange(startDate, endDate);

    LocalDateTime startDateTime = startDate.atStartOfDay();
    LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

    Double overallPlatformRating = calculateAverageRating();
    Long totalRatingsSubmitted = ratingRepository.countByCreatedAtBetween(startDateTime, endDateTime);
    List<AdminRatingAnalyticsDTO.RatingDistribution> ratingDistribution = getRatingDistribution();
    List<AdminRatingAnalyticsDTO.RatingTrend> averageRatingTrend =
        calculateAverageRatingTrend(startDateTime, endDateTime);

    List<AdminRatingAnalyticsDTO.TopRatedCharger> topRatedChargers = getTopRatedChargersForRating(10);
    List<AdminRatingAnalyticsDTO.TopRatedCharger> lowestRatedChargers = getLowestRatedChargers(10);
    List<AdminRatingAnalyticsDTO.BrandRating> ratingDistributionByBrand = getRatingDistributionByBrand();
    List<AdminRatingAnalyticsDTO.FrequentRater> usersWhoRateMostFrequently = getFrequentRaters(10);

    Long totalReviewsWithComments = ratingRepository.countByCommentIsNotNull();
    List<AdminRatingAnalyticsDTO.RecentReview> mostRecentReviews = getMostRecentReviews(20);

    return AdminRatingAnalyticsDTO.builder()
            .overallPlatformRating(overallPlatformRating)
            .totalRatingsSubmitted(totalRatingsSubmitted)
            .ratingDistribution(ratingDistribution)
            .averageRatingTrend(averageRatingTrend)
            .topRatedChargers(topRatedChargers)
            .lowestRatedChargers(lowestRatedChargers)
            .ratingDistributionByBrand(ratingDistributionByBrand)
            .usersWhoRateMostFrequently(usersWhoRateMostFrequently)
            .totalReviewsWithComments(totalReviewsWithComments)
            .mostRecentReviews(mostRecentReviews)
            .build();
}

    private List<AdminRatingAnalyticsDTO.RatingDistribution> getRatingDistribution() {
        Long total = ratingRepository.count();

        return List.of(1, 2, 3, 4, 5).stream()
                .map(stars -> {
                    Long count = ratingRepository.countByRatingScore(stars);
                    Double percentage = total > 0 ? (count.doubleValue() / total.doubleValue()) * 100 : 0.0;

                    return AdminRatingAnalyticsDTO.RatingDistribution.builder()
                            .stars(stars)
                            .count(count)
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<AdminRatingAnalyticsDTO.RatingTrend> calculateAverageRatingTrend(
            LocalDateTime start, LocalDateTime end) {
        List<AdminRatingAnalyticsDTO.RatingTrend> trend = new ArrayList<>();
        LocalDate currentDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        while (!currentDate.isAfter(endDate)) {
            LocalDateTime dayStart = currentDate.atStartOfDay();
            LocalDateTime dayEnd = currentDate.atTime(23, 59, 59);

            List<Rating> dayRatings = ratingRepository.findByCreatedAtBetween(dayStart, dayEnd);

            Double averageRating = dayRatings.isEmpty() ? 0.0 :
                dayRatings.stream()
                    .mapToInt(Rating::getRatingScore)
                    .average()
                    .orElse(0.0);

            trend.add(AdminRatingAnalyticsDTO.RatingTrend.builder()
                    .date(currentDate.format(DATE_FORMATTER))
                    .averageRating(averageRating)
                    .ratingCount((long) dayRatings.size())
                    .build());

            currentDate = currentDate.plusDays(1);
        }

        return trend;
    }

   private List<AdminRatingAnalyticsDTO.TopRatedCharger> getTopRatedChargersForRating(int limit) {
    List<Charger> chargers = chargerRepository.findAll();

    return chargers.stream()
            .filter(charger -> charger.getRating() != null && charger.getRating() > 0) // FIXED
            .map(charger -> {
                Long ratingCount = ratingRepository.countByChargerId(charger.getId());
                String hostName = charger.getHost() != null ?
                    charger.getHost().getFirstName() + " " + charger.getHost().getLastName() : "N/A";

                return AdminRatingAnalyticsDTO.TopRatedCharger.builder()
                        .chargerId(charger.getId())
                        .chargerName(charger.getName())
                        .brand(charger.getBrand())
                        .location(charger.getLocation())
                        .averageRating(charger.getRating() != null ? charger.getRating() : 0.0) // FIXED
                        .ratingCount(ratingCount)
                        .hostName(hostName)
                        .build();
            })
            .sorted(Comparator.comparing(AdminRatingAnalyticsDTO.TopRatedCharger::getAverageRating).reversed())
            .limit(limit)
            .collect(Collectors.toList());
}


    private List<AdminRatingAnalyticsDTO.TopRatedCharger> getLowestRatedChargers(int limit) {
    List<Charger> chargers = chargerRepository.findAll();

    return chargers.stream()
            .filter(charger -> charger.getRating() != null && charger.getRating() > 0) // FIXED
            .map(charger -> {
                Long ratingCount = ratingRepository.countByChargerId(charger.getId());
                String hostName = charger.getHost() != null ?
                    charger.getHost().getFirstName() + " " + charger.getHost().getLastName() : "N/A";

                return AdminRatingAnalyticsDTO.TopRatedCharger.builder()
                        .chargerId(charger.getId())
                        .chargerName(charger.getName())
                        .brand(charger.getBrand())
                        .location(charger.getLocation())
                        .averageRating(charger.getRating() != null ? charger.getRating() : 0.0) // FIXED
                        .ratingCount(ratingCount)
                        .hostName(hostName)
                        .build();
            })
            .sorted(Comparator.comparing(AdminRatingAnalyticsDTO.TopRatedCharger::getAverageRating))
            .limit(limit)
            .collect(Collectors.toList());
}

    private List<AdminRatingAnalyticsDTO.BrandRating> getRatingDistributionByBrand() {
        List<Charger> chargers = chargerRepository.findAll();

        // Filter out chargers with null brand to avoid NullPointerException in groupingBy
        Map<String, List<Charger>> chargersByBrand = chargers.stream()
                .filter(c -> c.getBrand() != null)
                .collect(Collectors.groupingBy(Charger::getBrand));

        return chargersByBrand.entrySet().stream()
                .map(entry -> {
                    String brand = entry.getKey();
                    List<Charger> brandChargers = entry.getValue();

                    // Null-safe rating filter to avoid NPE on unboxing
                    double avgRating = brandChargers.stream()
                            .filter(c -> c.getRating() != null && c.getRating() > 0)
                            .mapToDouble(Charger::getRating)
                            .average()
                            .orElse(0.0);

                    long ratingCount = brandChargers.stream()
                            .mapToLong(c -> ratingRepository.countByChargerId(c.getId()))
                            .sum();

                    return AdminRatingAnalyticsDTO.BrandRating.builder()
                            .brand(brand)
                            .averageRating(avgRating)
                            .ratingCount(ratingCount)
                            .build();
                })
                // Null-safe comparator to avoid NPE on getAverageRating()
                .sorted(Comparator.comparingDouble(
                        (AdminRatingAnalyticsDTO.BrandRating b) -> b.getAverageRating() != null ? b.getAverageRating() : 0.0)
                        .reversed())
                .collect(Collectors.toList());
    }

    private List<AdminRatingAnalyticsDTO.FrequentRater> getFrequentRaters(int limit) {
    List<Object[]> results = ratingRepository.findTopRatersByCount(limit);

    return results.stream()
            .map(result -> {
                User user = result[0] != null ? (User) result[0] : null;
                Long ratingCount = result[1] != null ? ((Number) result[1]).longValue() : 0L;
                Double avgRating = result[2] != null ? ((Number) result[2]).doubleValue() : 0.0;

                String userName = "Unknown User";
                Long userId = null;
                
                try {
                    if (user != null) {
                        userId = user.getUserId();
                        String firstName = user.getFirstName();
                        String lastName = user.getLastName();
                        userName = (firstName != null ? firstName : "") + " " + 
                                  (lastName != null ? lastName : "");
                        userName = userName.trim();
                        if (userName.isEmpty()) {
                            userName = "User #" + userId;
                        }
                    }
                } catch (Exception e) {
                    // Handle any access issues
                    userName = userId != null ? "User #" + userId : "Unknown User";
                }

                return AdminRatingAnalyticsDTO.FrequentRater.builder()
                        .userId(userId)
                        .userName(userName)
                        .ratingCount(ratingCount)
                        .averageRatingGiven(avgRating)
                        .build();
            })
            .collect(Collectors.toList());
}
    private List<AdminRatingAnalyticsDTO.RecentReview> getMostRecentReviews(int limit) {
    List<Rating> recentRatings = ratingRepository.findTop20ByOrderByCreatedAtDesc();

    return recentRatings.stream()
            .limit(limit)
            .map(rating -> {
                String userName = "Unknown User";
                String chargerName = "Unknown Charger";
                
                // Null-safe access to lazy-loaded entities
                try {
                    if (rating.getUser() != null) {
                        String firstName = rating.getUser().getFirstName();
                        String lastName = rating.getUser().getLastName();
                        userName = (firstName != null ? firstName : "") + " " + 
                                  (lastName != null ? lastName : "");
                        userName = userName.trim();
                        if (userName.isEmpty()) {
                            userName = "User";
                        }
                    }
                } catch (Exception e) {
                    // Handle LazyInitializationException or other errors
                    userName = "User";
                }
                
                try {
                    if (rating.getCharger() != null) {
                        String name = rating.getCharger().getName();
                        chargerName = name != null ? name : "Charger";
                    }
                } catch (Exception e) {
                    // Handle LazyInitializationException or other errors
                    chargerName = "Charger";
                }
                
                return AdminRatingAnalyticsDTO.RecentReview.builder()
                        .ratingId(rating.getId())
                        .userName(userName)
                        .chargerName(chargerName)
                        .ratingScore(rating.getRatingScore())
                        .comment(rating.getComment())
                        .createdAt(rating.getCreatedAt())
                        .build();
            })
            .collect(Collectors.toList());
}


    // ========================================================================
    // Platform Performance Analytics
    // ========================================================================

    @Override
    public AdminPlatformPerformanceDTO getPlatformPerformanceAnalytics(LocalDate startDate, LocalDate endDate) {
        validationService.validateDateRange(startDate, endDate);

        Long totalTransactionsProcessed = paymentRepository.count();
        Long totalNotificationsSent = notificationRepository.count();

        Long dailyActiveUsers = userRepository.countUsersWithBookingsAfter(
            LocalDateTime.now().minusDays(1));
        Long weeklyActiveUsers = userRepository.countUsersWithBookingsAfter(
            LocalDateTime.now().minusDays(7));
        Long monthlyActiveUsers = userRepository.countUsersWithBookingsAfter(
            LocalDateTime.now().minusDays(30));
        Double notificationDeliveryRate = 100.0;

        AdminPlatformPerformanceDTO.GrowthMetrics monthOverMonthGrowth =
            calculateMonthOverMonthGrowth();
        AdminPlatformPerformanceDTO.GrowthMetrics yearOverYearGrowth =
            calculateYearOverYearGrowth();
        Double platformAdoptionRate = calculatePlatformAdoptionRate();
        Double userToBookingConversionRate = calculateUserToBookingConversionRate();

        return AdminPlatformPerformanceDTO.builder()
                .totalTransactionsProcessed(totalTransactionsProcessed)
                .totalNotificationsSent(totalNotificationsSent)
                .dailyActiveUsers(dailyActiveUsers)
                .weeklyActiveUsers(weeklyActiveUsers)
                .monthlyActiveUsers(monthlyActiveUsers)
                .notificationDeliveryRate(notificationDeliveryRate)
                .monthOverMonthGrowth(monthOverMonthGrowth)
                .yearOverYearGrowth(yearOverYearGrowth)
                .platformAdoptionRate(platformAdoptionRate)
                .userToBookingConversionRate(userToBookingConversionRate)
                .build();
    }

    private AdminPlatformPerformanceDTO.GrowthMetrics calculateMonthOverMonthGrowth() {
        LocalDateTime thisMonthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime lastMonthStart = thisMonthStart.minusMonths(1);
        LocalDateTime lastMonthEnd = thisMonthStart.minusNanos(1);

        Long thisMonthUsers = userRepository.countByCreatedAtBetween(thisMonthStart, LocalDateTime.now());
        Long lastMonthUsers = userRepository.countByCreatedAtBetween(lastMonthStart, lastMonthEnd);
        Double userGrowth = calculateGrowthPercentage(lastMonthUsers, thisMonthUsers);

        Long thisMonthHosts = userRepository.countByRoleAndCreatedAtBetween(Role.HOST, thisMonthStart, LocalDateTime.now());
        Long lastMonthHosts = userRepository.countByRoleAndCreatedAtBetween(Role.HOST, lastMonthStart, lastMonthEnd);
        Double hostGrowth = calculateGrowthPercentage(lastMonthHosts, thisMonthHosts);

        Long thisMonthBookings = bookingRepository.countByCreatedAtBetween(thisMonthStart, LocalDateTime.now());
        Long lastMonthBookings = bookingRepository.countByCreatedAtBetween(lastMonthStart, lastMonthEnd);
        Double bookingGrowth = calculateGrowthPercentage(lastMonthBookings, thisMonthBookings);

        BigDecimal thisMonthRevenue = calculateTotalRevenue(thisMonthStart, LocalDateTime.now());
        BigDecimal lastMonthRevenue = calculateTotalRevenue(lastMonthStart, lastMonthEnd);
        Double revenueGrowth = calculateRevenueGrowthPercentage(lastMonthRevenue, thisMonthRevenue);

        return AdminPlatformPerformanceDTO.GrowthMetrics.builder()
                .userGrowth(userGrowth)
                .hostGrowth(hostGrowth)
                .bookingGrowth(bookingGrowth)
                .revenueGrowth(revenueGrowth)
                .build();
    }

    private AdminPlatformPerformanceDTO.GrowthMetrics calculateYearOverYearGrowth() {
        LocalDateTime thisYearStart = LocalDate.now().withDayOfYear(1).atStartOfDay();
        LocalDateTime lastYearStart = thisYearStart.minusYears(1);
        LocalDateTime lastYearEnd = thisYearStart.minusNanos(1);

        Long thisYearUsers = userRepository.countByCreatedAtBetween(thisYearStart, LocalDateTime.now());
        Long lastYearUsers = userRepository.countByCreatedAtBetween(lastYearStart, lastYearEnd);
        Double userGrowth = calculateGrowthPercentage(lastYearUsers, thisYearUsers);

        Long thisYearHosts = userRepository.countByRoleAndCreatedAtBetween(Role.HOST, thisYearStart, LocalDateTime.now());
        Long lastYearHosts = userRepository.countByRoleAndCreatedAtBetween(Role.HOST, lastYearStart, lastYearEnd);
        Double hostGrowth = calculateGrowthPercentage(lastYearHosts, thisYearHosts);

        Long thisYearBookings = bookingRepository.countByCreatedAtBetween(thisYearStart, LocalDateTime.now());
        Long lastYearBookings = bookingRepository.countByCreatedAtBetween(lastYearStart, lastYearEnd);
        Double bookingGrowth = calculateGrowthPercentage(lastYearBookings, thisYearBookings);

        BigDecimal thisYearRevenue = calculateTotalRevenue(thisYearStart, LocalDateTime.now());
        BigDecimal lastYearRevenue = calculateTotalRevenue(lastYearStart, lastYearEnd);
        Double revenueGrowth = calculateRevenueGrowthPercentage(lastYearRevenue, thisYearRevenue);

        return AdminPlatformPerformanceDTO.GrowthMetrics.builder()
                .userGrowth(userGrowth)
                .hostGrowth(hostGrowth)
                .bookingGrowth(bookingGrowth)
                .revenueGrowth(revenueGrowth)
                .build();
    }

    private Double calculateGrowthPercentage(Long previous, Long current) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return ((current - previous) / (double) previous) * 100;
    }

    private Double calculateRevenueGrowthPercentage(BigDecimal previous, BigDecimal current) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private Double calculatePlatformAdoptionRate() {
        Long totalUsers = userRepository.count();
        Long activeUsers = userRepository.countUsersWithBookings();

        if (totalUsers == 0) return 0.0;
        return (activeUsers.doubleValue() / totalUsers.doubleValue()) * 100;
    }

    private Double calculateUserToBookingConversionRate() {
        Long totalUsers = userRepository.countByRole(Role.USER);
        Long usersWithBookings = userRepository.countUsersWithBookings();

        if (totalUsers == 0) return 0.0;
        return (usersWithBookings.doubleValue() / totalUsers.doubleValue()) * 100;
    }

    // ========================================================================
    // Time Analytics
    // ========================================================================

    @Override
    public AdminTimeAnalyticsDTO getTimeAnalytics(LocalDate startDate, LocalDate endDate) {
        validationService.validateDateRange(startDate, endDate);

        List<AdminTimeAnalyticsDTO.DayPattern> busiestDaysOfWeek = getBusiestDaysOfWeek();
        List<AdminTimeAnalyticsDTO.HourPattern> peakBookingHoursForTime = getPeakBookingHoursForTime();
        List<AdminTimeAnalyticsDTO.SeasonalTrend> seasonalTrends = getSeasonalTrends();
        AdminTimeAnalyticsDTO.WeekendVsWeekday weekendVsWeekdayPerformance =
            getWeekendVsWeekdayPerformance();

        return AdminTimeAnalyticsDTO.builder()
                .busiestDaysOfWeek(busiestDaysOfWeek)
                .peakBookingHours(peakBookingHoursForTime)
                .seasonalTrends(seasonalTrends)
                .weekendVsWeekdayPerformance(weekendVsWeekdayPerformance)
                .build();
    }

    private List<AdminTimeAnalyticsDTO.DayPattern> getBusiestDaysOfWeek() {
        List<Booking> bookings = bookingRepository.findAll();

        Map<DayOfWeek, Long> dayCounts = bookings.stream()
                .collect(Collectors.groupingBy(
                    booking -> booking.getStartTime().getDayOfWeek(),
                    Collectors.counting()
                ));

        Long totalBookings = (long) bookings.size();

        return dayCounts.entrySet().stream()
                .map(entry -> {
                    Double utilization = totalBookings > 0 ?
                        (entry.getValue().doubleValue() / totalBookings.doubleValue()) * 100 : 0.0;

                    return AdminTimeAnalyticsDTO.DayPattern.builder()
                            .dayOfWeek(entry.getKey().toString())
                            .bookingCount(entry.getValue())
                            .averageUtilization(utilization)
                            .build();
                })
                .sorted(Comparator.comparing(AdminTimeAnalyticsDTO.DayPattern::getBookingCount).reversed())
                .collect(Collectors.toList());
    }

    private List<AdminTimeAnalyticsDTO.HourPattern> getPeakBookingHoursForTime() {
        List<Booking> bookings = bookingRepository.findAll();

        Map<Integer, Long> hourCounts = bookings.stream()
                .collect(Collectors.groupingBy(
                    booking -> booking.getStartTime().getHour(),
                    Collectors.counting()
                ));

        return hourCounts.entrySet().stream()
                .map(entry -> {
                    String timeLabel = String.format("%02d:00 - %02d:00", entry.getKey(), entry.getKey() + 1);

                    return AdminTimeAnalyticsDTO.HourPattern.builder()
                            .hour(entry.getKey())
                            .bookingCount(entry.getValue())
                            .timeLabel(timeLabel)
                            .build();
                })
                .sorted(Comparator.comparing(AdminTimeAnalyticsDTO.HourPattern::getHour))
                .collect(Collectors.toList());
    }

    private List<AdminTimeAnalyticsDTO.SeasonalTrend> getSeasonalTrends() {
        List<Booking> bookings = bookingRepository.findAll();

        Map<String, List<Booking>> seasonalBookings = bookings.stream()
                .collect(Collectors.groupingBy(booking -> {
                    int month = booking.getStartTime().getMonthValue();
                    if (month >= 3 && month <= 5) return "Spring";
                    if (month >= 6 && month <= 8) return "Summer";
                    if (month >= 9 && month <= 11) return "Fall";
                    return "Winter";
                }));

        return seasonalBookings.entrySet().stream()
                .map(entry -> {
                    List<Booking> seasonBookings = entry.getValue();
                    double avgRating = seasonBookings.stream()
                            .map(Booking::getCharger)
                            .mapToDouble(Charger::getRating)
                            .average()
                            .orElse(0.0);

                    return AdminTimeAnalyticsDTO.SeasonalTrend.builder()
                            .period(entry.getKey())
                            .bookingCount((long) seasonBookings.size())
                            .averageRating(avgRating)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private AdminTimeAnalyticsDTO.WeekendVsWeekday getWeekendVsWeekdayPerformance() {
        List<Booking> bookings = bookingRepository.findAll();

        List<Booking> weekdayBookings = bookings.stream()
                .filter(b -> {
                    DayOfWeek day = b.getStartTime().getDayOfWeek();
                    return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
                })
                .collect(Collectors.toList());

        List<Booking> weekendBookings = bookings.stream()
                .filter(b -> {
                    DayOfWeek day = b.getStartTime().getDayOfWeek();
                    return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
                })
                .collect(Collectors.toList());

        Double weekdayRevenue = weekdayBookings.stream()
                .map(Booking::getTotalPrice)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();

        Double weekendRevenue = weekendBookings.stream()
                .map(Booking::getTotalPrice)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();

        return AdminTimeAnalyticsDTO.WeekendVsWeekday.builder()
                .weekdayBookings((long) weekdayBookings.size())
                .weekendBookings((long) weekendBookings.size())
                .weekdayRevenue(weekdayRevenue)
                .weekendRevenue(weekendRevenue)
                .build();
    }

    // ========================================================================
    // Host Revenue & Rating Helpers
    // ========================================================================

    /**
     * Calculate total revenue earned by all hosts from completed bookings.
     */
    private BigDecimal calculateTotalHostRevenue() {
        List<Booking> completedBookings = bookingRepository.findByStatus(BookingStatus.COMPLETED);

        return completedBookings.stream()
                .map(Booking::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

   private Double calculateAverageHostRating() {
    List<Charger> allChargers = chargerRepository.findAll();

    if (allChargers.isEmpty()) {
        return 0.0;
    }

    double totalRating = allChargers.stream()
            .map(Charger::getRating)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .sum();
    
    long nonNullRatingsCount = allChargers.stream()
            .map(Charger::getRating)
            .filter(Objects::nonNull)
            .count();

    return nonNullRatingsCount > 0 ? totalRating / nonNullRatingsCount : 0.0;
}
}