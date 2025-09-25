package com.evstation.ev_charging_backend.controller;

import com.evstation.ev_charging_backend.dto.DashboardResponse;
import com.evstation.ev_charging_backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(Authentication authentication) {
        String email = authentication.getName(); // comes from JWT token
        return ResponseEntity.ok(dashboardService.getDashboardData(email));
    }
}
