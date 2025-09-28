package com.evstation.ev_charging_backend.controller;

import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.enums.Role;
import com.evstation.ev_charging_backend.repository.UserRepository;
import com.evstation.ev_charging_backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // 1️⃣ Get all pending hosts
    @GetMapping("/pending-hosts")
    public ResponseEntity<List<User>> getPendingHosts() {
        List<User> pendingHosts = userRepository.findByRole(Role.PENDING_HOST);
        return ResponseEntity.ok(pendingHosts);
    }

    // 2️⃣ Approve a pending host by ID
    @PutMapping("/approve-host/{id}")
    public ResponseEntity<Map<String, Object>> approveHost(@PathVariable Long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        }

        User user = optionalUser.get();
        user.setRole(Role.HOST);
        userRepository.save(user);

        // Generate JWT token for host after approval
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return ResponseEntity.ok(Map.of(
                "message", "Host approved successfully!",
                "email", user.getEmail(),
                "role", user.getRole(),
                "token", token));
    }

    // 3️⃣ Reject a pending host by ID (Delete User)
    @DeleteMapping("/reject-host/{id}")
    public ResponseEntity<Map<String, Object>> rejectHost(@PathVariable Long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        }

        User user = optionalUser.get();

        if (user.getRole() != Role.PENDING_HOST) {
            return ResponseEntity.badRequest().body(Map.of("message", "User is not a pending host"));
        }

        userRepository.delete(user);

        return ResponseEntity.ok(Map.of(
                "message", "Host rejected and deleted successfully!",
                "deletedUserId", id));
    }

}
