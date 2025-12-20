package com.evstation.ev_charging_backend.entity;

import com.evstation.ev_charging_backend.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "bookings",
    indexes = {
        @Index(name = "idx_booking_charger_time", columnList = "charger_id,start_time,end_time"),
        @Index(name = "idx_booking_user", columnList = "user_id"),
        @Index(name = "idx_booking_status_start", columnList = "status,start_time")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Who booked
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 🔌 Which charger
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "charger_id", nullable = false)
    private Charger charger;

    // ⏱ Time slot
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // 📊 Booking lifecycle
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    // 💰 Snapshot pricing
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerKwh;

    // Energy consumed (populated after charging session)
    private Double totalEnergyKwh;

    // Total cost (calculated after charging)
    @Column(precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}