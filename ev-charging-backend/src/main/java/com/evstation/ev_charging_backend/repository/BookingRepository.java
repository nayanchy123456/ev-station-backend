package com.evstation.ev_charging_backend.repository;

import com.evstation.ev_charging_backend.entity.Booking;
import com.evstation.ev_charging_backend.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // 🔒 Check overlapping bookings for same charger
    @Query("""
        SELECT b FROM Booking b
        WHERE b.charger.id = :chargerId
        AND b.status IN :activeStatuses
        AND (:startTime < b.endTime AND :endTime > b.startTime)
    """)
    List<Booking> findConflictingBookings(
            @Param("chargerId") Long chargerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("activeStatuses") List<BookingStatus> activeStatuses
    );

    // 👤 User bookings
    List<Booking> findByUserUserIdOrderByStartTimeDesc(Long userId);

    // 🔌 Charger bookings
    List<Booking> findByChargerIdOrderByStartTimeAsc(Long chargerId);
    
    // 🔍 Find booking with pessimistic lock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);
}