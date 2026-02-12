package com.evstation.ev_charging_backend.repository;

import com.evstation.ev_charging_backend.entity.Payment;
import com.evstation.ev_charging_backend.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findByTransactionId(String transactionId);

    // Cleanup old pending payments
    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime time);

    // Find payments by user (through booking relationship)
    List<Payment> findByBookingUserUserIdOrderByCreatedAtDesc(Long userId);


    // Add these methods to PaymentRepository interface:

    List<Payment> findByStatusAndCreatedAtBetween(PaymentStatus status, LocalDateTime start, LocalDateTime end);
    
    List<Payment> findByStatus(PaymentStatus status);
    
    Long countByStatus(PaymentStatus status);


    /**
 * Get daily revenue trend in a single aggregated query
 * Returns: [date, revenue]
 */
@Query(value = """
    SELECT DATE(p.created_at) as date, 
           COALESCE(SUM(p.amount), 0) as revenue
    FROM payments p
    WHERE p.status = 'SUCCESS'
      AND p.created_at BETWEEN :startDate AND :endDate
    GROUP BY DATE(p.created_at)
    ORDER BY DATE(p.created_at)
    """, nativeQuery = true)
List<Object[]> getDailyRevenueTrend(
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate
);

/**
 * Get monthly revenue trend in a single aggregated query
 * Returns: [month, revenue]
 */
@Query(value = """
    SELECT DATE_FORMAT(p.created_at, '%Y-%m') as month,
           COALESCE(SUM(p.amount), 0) as revenue
    FROM payments p
    WHERE p.status = 'SUCCESS'
      AND p.created_at BETWEEN :startDate AND :endDate
    GROUP BY DATE_FORMAT(p.created_at, '%Y-%m')
    ORDER BY DATE_FORMAT(p.created_at, '%Y-%m')
    """, nativeQuery = true)
List<Object[]> getMonthlyRevenueTrend(
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate
);
}