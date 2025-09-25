package com.evstation.ev_charging_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponse {
    private String message;
    private Object data; // can hold UserDashboardResponse or AdminDashboardResponse
}
