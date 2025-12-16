package com.evstation.ev_charging_backend.dto;

import com.evstation.ev_charging_backend.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^[8-9]\\d{9}$|^\\+?977[8-9]\\d{9}$",
        message = "Phone number must be a valid Nepal number with exactly 10 digits (starting with 8 or 9), optionally with country code (+977)"
    )
    private String phone;
    
    private String password;
    private Role role;
}
