package com.evstation.ev_charging_backend.repository;

import com.evstation.ev_charging_backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    long countByUserUserIdAndReadFalse(Long userId);
}