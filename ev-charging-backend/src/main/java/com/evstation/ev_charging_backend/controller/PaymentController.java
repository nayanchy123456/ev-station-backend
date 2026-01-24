package com.evstation.ev_charging_backend.controller;

import com.evstation.ev_charging_backend.dto.*;
import com.evstation.ev_charging_backend.security.JwtUtil;
import com.evstation.ev_charging_backend.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtUtil jwtUtil;

    public PaymentController(PaymentService paymentService, JwtUtil jwtUtil) {
        this.paymentService = paymentService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiateResponseDto> initiatePayment(
            @Valid @RequestBody PaymentInitiateRequestDto dto,
            HttpServletRequest request
    ) {
        Long userId = extractUserId(request);
        PaymentInitiateResponseDto response = paymentService.initiatePayment(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/process")
    public ResponseEntity<PaymentProcessResponseDto> processPayment(
            @Valid @RequestBody PaymentProcessRequestDto dto,
            HttpServletRequest request
    ) {
        Long userId = extractUserId(request);
        PaymentProcessResponseDto response = paymentService.processPayment(dto, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refund/{paymentId}")
    public ResponseEntity<CancellationResponseDto> refundPayment(
            @PathVariable Long paymentId,
            @RequestBody(required = false) CancellationRequestDto dto,
            HttpServletRequest request
    ) {
        Long userId = extractUserId(request);
        String reason = dto != null ? dto.getReason() : null;
        CancellationResponseDto response = paymentService.processRefund(paymentId, userId, reason);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentProcessResponseDto> getPayment(
            @PathVariable Long paymentId,
            HttpServletRequest request
    ) {
        Long userId = extractUserId(request);
        PaymentProcessResponseDto response = paymentService.getPaymentById(paymentId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentProcessResponseDto> getPaymentByBooking(
            @PathVariable Long bookingId,
            HttpServletRequest request
    ) {
        Long userId = extractUserId(request);
        PaymentProcessResponseDto response = paymentService.getPaymentByBooking(bookingId, userId);
        return ResponseEntity.ok(response);
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