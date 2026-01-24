package com.evstation.ev_charging_backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String message;
    private String token;
    private String email;
    private String role;
    private String firstName;
    private String lastName;
    private String phone;
    private Long userId;  // ✅ Make sure this field exists
    private LocalDateTime createdAt;

    // Constructor for error messages
    public AuthResponse(String message) {
        this.message = message;
    }
}