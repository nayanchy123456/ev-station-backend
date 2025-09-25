package com.evstation.ev_charging_backend.controller;

import com.evstation.ev_charging_backend.dto.AuthResponse;
import com.evstation.ev_charging_backend.dto.LoginRequest;
import com.evstation.ev_charging_backend.dto.RegisterRequest;
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
        // userService.login now generates JWT token and returns in AuthResponse
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

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
