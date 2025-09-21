package com.evstation.ev_charging_backend.dto;

import com.evstation.ev_charging_backend.enums.RoleType;

public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private RoleType role;
}
