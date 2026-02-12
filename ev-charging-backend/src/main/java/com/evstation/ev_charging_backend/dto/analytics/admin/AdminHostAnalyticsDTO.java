package com.evstation.ev_charging_backend.dto.analytics.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminHostAnalyticsDTO {
    
    // ===== HOST STATISTICS (FOR FRONTEND CARDS) =====
    private Long totalHosts;              // Total approved hosts
    private Long activeHosts;             // Hosts with active chargers
    private Long pendingApprovals;        // Pending host approvals
    private Double avgChargersPerHost;    // Average chargers per host
    private BigDecimal totalHostRevenue;  // Total revenue earned by all hosts
    private Double avgHostRating;         // Average rating across all host chargers
    
    // ===== LEGACY FIELDS (backward compatibility) =====
    private Long totalApprovedHosts;
    private Long pendingHostApprovals;
    private Long rejectedHosts;
    private Double averageChargersPerHost;
    private List<HostApprovalTimeline> hostApprovalTimeline;
    
    // ===== HOST PERFORMANCE =====
    private List<TopHost> topPerformingHostsByRevenue;
    private List<TopHost> topRatedHosts;
    private List<TopHost> hostsWithMostBookings;
    
    // ===== HOST MANAGEMENT METRICS =====
    private Double averageApprovalTimeDays;
    private Double pendingToApprovedRatio;
    private Double hostChurnRate;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HostApprovalTimeline {
        private String date;
        private Long approved;
        private Long rejected;
        private Long pending;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopHost {
        private Long hostId;
        private String name;
        private String email;
        private BigDecimal totalRevenue;
        private Long bookingCount;
        private Double averageRating;
        private Integer chargerCount;
        private LocalDateTime joinedDate;
    }
}