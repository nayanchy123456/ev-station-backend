package com.evstation.ev_charging_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDashboardResponse {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String role;
    private LocalDateTime createdAt;
}
