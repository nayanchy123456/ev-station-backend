package com.evstation.ev_charging_backend.controller;

import com.evstation.ev_charging_backend.dto.ReservationRequestDto;
import com.evstation.ev_charging_backend.dto.ReservationResponseDto;
import com.evstation.ev_charging_backend.security.JwtUtil;
import com.evstation.ev_charging_backend.service.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final JwtUtil jwtUtil;

    public ReservationController(ReservationService reservationService, JwtUtil jwtUtil) {
        this.reservationService = reservationService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/create")
    public ResponseEntity<ReservationResponseDto> createReservation(
            @Valid @RequestBody ReservationRequestDto dto,
            HttpServletRequest request
    ) {
        Long userId = extractUserId(request);
        ReservationResponseDto response = reservationService.createReservation(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponseDto> getReservation(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        Long userId = extractUserId(request);
        ReservationResponseDto response = reservationService.getReservationById(id, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReservationResponseDto>> getMyReservations(HttpServletRequest request) {
        Long userId = extractUserId(request);
        List<ReservationResponseDto> reservations = reservationService.getMyReservations(userId);
        return ResponseEntity.ok(reservations);
    }

    private Long extractUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);
        if (!jwtUtil.validateToken(token, username)) {
            throw new SecurityException("Invalid or expired token");
        }
        Long userId = jwtUtil.extractUserId(token);
        if (userId == null) {
            throw new SecurityException("User ID not found in token");
        }
        return userId;
    }
}