package com.evstation.ev_charging_backend.controller;

import com.evstation.ev_charging_backend.dto.AuthResponse;
import com.evstation.ev_charging_backend.dto.LoginRequest;
import com.evstation.ev_charging_backend.dto.RegisterRequest;
import com.evstation.ev_charging_backend.enums.Role;
import com.evstation.ev_charging_backend.exception.InvalidPhoneNumberException;
import com.evstation.ev_charging_backend.security.JwtUtil;
import com.evstation.ev_charging_backend.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    // Register new user
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = userService.register(request);
            return ResponseEntity.ok(response);
        } catch (InvalidPhoneNumberException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(e.getMessage()));
        }
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

    // Issues a fresh JWT from an expired one. Called automatically by the
    // frontend's axios interceptor whenever an API call returns 401.
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing token"));
        }

        String oldToken = authHeader.substring(7);

        try {
            Claims claims = jwtUtil.extractClaimsAllowExpired(oldToken);
            String username = claims.getSubject();
            String role = (String) claims.get("role");
            Object userIdClaim = claims.get("userId");
            Long userId = userIdClaim == null ? null
                    : (userIdClaim instanceof Integer ? ((Integer) userIdClaim).longValue() : (Long) userIdClaim);

            String newToken = jwtUtil.generateToken(username, role, userId);

            Map<String, String> response = new HashMap<>();
            response.put("token", newToken);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
    }
}