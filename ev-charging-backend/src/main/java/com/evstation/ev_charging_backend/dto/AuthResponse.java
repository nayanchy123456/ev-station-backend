package com.evstation.ev_charging_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String message;
    private String token;
    private String email;
    private String role;
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDateTime createdAt;
}
