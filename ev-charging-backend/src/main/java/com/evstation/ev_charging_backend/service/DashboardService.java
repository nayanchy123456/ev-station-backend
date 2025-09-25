package com.evstation.ev_charging_backend.service;

import com.evstation.ev_charging_backend.dto.DashboardResponse;

public interface DashboardService {
    DashboardResponse getDashboardData(String email);
}
