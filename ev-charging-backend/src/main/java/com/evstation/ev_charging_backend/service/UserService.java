package com.evstation.ev_charging_backend.service;

import com.evstation.ev_charging_backend.dto.RegisterRequest;
import com.evstation.ev_charging_backend.entity.User;

public interface UserService {

    User registerUser(RegisterRequest request);
}
