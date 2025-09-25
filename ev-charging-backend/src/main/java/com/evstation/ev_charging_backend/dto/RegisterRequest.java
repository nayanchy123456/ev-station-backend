package com.evstation.ev_charging_backend.dto;

import com.evstation.ev_charging_backend.enums.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String password;
    private Role role;
}
