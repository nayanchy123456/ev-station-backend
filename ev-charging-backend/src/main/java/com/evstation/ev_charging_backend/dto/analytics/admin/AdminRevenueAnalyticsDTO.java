package com.evstation.ev_charging_backend.dto.analytics.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRevenueAnalyticsDTO {
    
    // Revenue Overview
    private BigDecimal totalRevenue;
    private Double revenueGrowthRate;
    private BigDecimal averageRevenuePerBooking;
    private List<MonthlyRevenue> revenueByMonth;
    
    // Revenue Breakdown
    private List<RevenueByCharger> revenueByCharger;
    private List<RevenueByHost> revenueByHost;
    private List<RevenueByLocation> revenueByLocation;
    
    // Financial Metrics
    private Double totalEnergyConsumedKwh;
    private BigDecimal averageTransactionValue;
    private BigDecimal revenuePerUser;
    private BigDecimal revenuePerCharger;
    
    // Payment Insights
    private Double paymentSuccessRate;
    private List<PaymentMethodDistribution> paymentMethodDistribution;
    private Long failedPaymentCount;
    private RefundStatistics refundStatistics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenue {
        private String month;
        private BigDecimal revenue;
        private Long bookingCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueByCharger {
        private Long chargerId;
        private String chargerName;
        private BigDecimal revenue;
        private Long bookingCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueByHost {
        private Long hostId;
        private String hostName;
        private BigDecimal revenue;
        private Long bookingCount;
        private Integer chargerCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueByLocation {
        private String location;
        private BigDecimal revenue;
        private Long bookingCount;
        private Integer chargerCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodDistribution {
        private String paymentMethod;
        private Long count;
        private BigDecimal totalAmount;
        private Double percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundStatistics {
        private Long totalRefunds;
        private BigDecimal totalRefundAmount;
        private Double refundRate;
    }
}