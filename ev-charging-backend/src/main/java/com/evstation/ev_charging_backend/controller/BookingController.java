package com.evstation.ev_charging_backend.controller;

import com.evstation.ev_charging_backend.dto.BookingRequestDto;
import com.evstation.ev_charging_backend.dto.BookingResponseDto;
import com.evstation.ev_charging_backend.security.JwtUtil;
import com.evstation.ev_charging_backend.service.BookingService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final JwtUtil jwtUtil;

    public BookingController(BookingService bookingService, JwtUtil jwtUtil) {
        this.bookingService = bookingService;   
        this.jwtUtil = jwtUtil;
    }

    // ================= CREATE BOOKING =================

    @PostMapping
    public ResponseEntity<BookingResponseDto> createBooking(
            @Valid @RequestBody BookingRequestDto dto,
            HttpServletRequest request
    ) {
        Long userId = extractUserId(request);
        BookingResponseDto response = bookingService.createBooking(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ================= MY BOOKINGS =================

    @GetMapping("/my")
    public ResponseEntity<List<BookingResponseDto>> getMyBookings(HttpServletRequest request) {
        Long userId = extractUserId(request);
        return ResponseEntity.ok(bookingService.getMyBookings(userId));
    }

    // ================= CHARGER BOOKINGS =================

    @GetMapping("/charger/{chargerId}")
    public ResponseEntity<List<BookingResponseDto>> getBookingsByCharger(
            @PathVariable Long chargerId
    ) {
        return ResponseEntity.ok(bookingService.getBookingsByCharger(chargerId));
    }

    // ================= CANCEL BOOKING =================

@PutMapping("/{bookingId}/cancel")
public ResponseEntity<Map<String, Object>> cancelBooking(
        @PathVariable Long bookingId,
        HttpServletRequest request
) {
    Long userId = extractUserId(request);
    bookingService.cancelBooking(bookingId, userId);
    
    return ResponseEntity.ok(Map.of(
        "message", "Booking cancelled successfully",
        "bookingId", bookingId,
        "timestamp", java.time.Instant.now()
    ));
}

    // ================= HELPER =================

    private Long extractUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        
        // Validate token with username
        String username = jwtUtil.extractUsername(token);
        if (!jwtUtil.validateToken(token, username)) {
            throw new SecurityException("Invalid or expired token");
        }
        
        // Extract and return userId
        Long userId = jwtUtil.extractUserId(token);
        if (userId == null) {
            throw new SecurityException("User ID not found in token");
        }
        
        return userId;
    }
}