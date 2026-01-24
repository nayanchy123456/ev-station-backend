package com.evstation.ev_charging_backend.controller;

import com.evstation.ev_charging_backend.dto.BookingResponseDto;
import com.evstation.ev_charging_backend.dto.CancellationRequestDto;
import com.evstation.ev_charging_backend.dto.CancellationResponseDto;
import com.evstation.ev_charging_backend.entity.Payment;
import com.evstation.ev_charging_backend.enums.PaymentStatus;
import com.evstation.ev_charging_backend.repository.PaymentRepository;
import com.evstation.ev_charging_backend.security.JwtUtil;
import com.evstation.ev_charging_backend.service.BookingService;
import com.evstation.ev_charging_backend.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final JwtUtil jwtUtil;

    public BookingController(
            BookingService bookingService,
            PaymentService paymentService,
            PaymentRepository paymentRepository,
            JwtUtil jwtUtil
    ) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/host")
    public ResponseEntity<List<BookingResponseDto>> getBookingsByHost(HttpServletRequest request) {
        Long hostId = extractUserId(request);
        List<BookingResponseDto> bookings = bookingService.getBookingsByHost(hostId);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/my")
    public ResponseEntity<List<BookingResponseDto>> getMyBookings(HttpServletRequest request) {
        Long userId = extractUserId(request);
        return ResponseEntity.ok(bookingService.getMyBookings(userId));
    }

    @GetMapping("/charger/{chargerId}")
    public ResponseEntity<List<BookingResponseDto>> getBookingsByCharger(
            @PathVariable Long chargerId
    ) {
        return ResponseEntity.ok(bookingService.getBookingsByCharger(chargerId));
    }

    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<CancellationResponseDto> cancelBooking(
            @PathVariable Long bookingId,
            @RequestBody(required = false) CancellationRequestDto dto,
            HttpServletRequest request
    ) {
        Long userId = extractUserId(request);

        // Check if there's a successful payment for this booking
        Optional<Payment> paymentOpt = paymentRepository.findByBookingId(bookingId);

        if (paymentOpt.isPresent() && paymentOpt.get().getStatus() == PaymentStatus.SUCCESS) {
            // Process refund through payment service
            String reason = dto != null ? dto.getReason() : "User cancelled booking";
            return ResponseEntity.ok(
                paymentService.processRefund(paymentOpt.get().getId(), userId, reason)
            );
        } else {
            // No payment or payment not successful, just cancel the booking
            bookingService.cancelBooking(bookingId, userId);
            return ResponseEntity.ok(
                CancellationResponseDto.builder()
                    .success(true)
                    .bookingId(bookingId)
                    .message("Booking cancelled successfully")
                    .refundProcessed(false)
                    .build()
            );
        }
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