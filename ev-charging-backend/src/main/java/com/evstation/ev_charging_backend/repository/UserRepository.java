package com.evstation.ev_charging_backend.repository;

import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    // ✅ Add this method to fetch users by role (e.g., PENDING_HOST)
    List<User> findByRole(Role role);
}
