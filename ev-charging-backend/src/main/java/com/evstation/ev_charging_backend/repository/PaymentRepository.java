package com.evstation.ev_charging_backend.repository;

import com.evstation.ev_charging_backend.entity.Payment;
import com.evstation.ev_charging_backend.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
}