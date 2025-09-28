package com.evstation.ev_charging_backend.controller;

import com.evstation.ev_charging_backend.dto.AuthResponse;
import com.evstation.ev_charging_backend.dto.LoginRequest;
import com.evstation.ev_charging_backend.dto.RegisterRequest;
import com.evstation.ev_charging_backend.enums.Role;
import com.evstation.ev_charging_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // Register new user
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return ResponseEntity.ok(response);
    }

    // Login and return JWT token
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);

        // ❌ Block PENDING_HOST from logging in
        if (response.getRole() != null && response.getRole().equals(Role.PENDING_HOST.name())) {
            return ResponseEntity.status(403) // Forbidden
                    .body(new AuthResponse("Your host registration is pending approval by admin."));
        }

        return ResponseEntity.ok(response);
    }

    // Get current user profile
    @GetMapping("/profile")
    public ResponseEntity<AuthResponse> getProfile() {
        try {
            AuthResponse response = userService.getCurrentUserProfile();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new AuthResponse("Failed to fetch profile: " + e.getMessage()));
        }
    }
}
