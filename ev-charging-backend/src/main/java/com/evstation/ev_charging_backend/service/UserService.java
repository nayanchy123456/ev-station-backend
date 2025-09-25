package com.evstation.ev_charging_backend.service;

import com.evstation.ev_charging_backend.dto.AuthResponse;
import com.evstation.ev_charging_backend.dto.LoginRequest;
import com.evstation.ev_charging_backend.dto.RegisterRequest;

public interface UserService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse getCurrentUserProfile();
}
