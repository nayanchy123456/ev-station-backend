package com.evstation.ev_charging_backend.controller;

import com.evstation.ev_charging_backend.dto.ReceiptDto;
import com.evstation.ev_charging_backend.security.JwtUtil;
import com.evstation.ev_charging_backend.service.ReceiptService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;
    private final JwtUtil jwtUtil;

    public ReceiptController(ReceiptService receiptService, JwtUtil jwtUtil) {
        this.receiptService = receiptService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/{receiptId}")
    public ResponseEntity<ReceiptDto> getReceipt(
            @PathVariable Long receiptId,
            HttpServletRequest request
    ) {
        Long userId = extractUserId(request);
        ReceiptDto receipt = receiptService.getReceiptById(receiptId, userId);
        return ResponseEntity.ok(receipt);
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ReceiptDto> getReceiptByBooking(
            @PathVariable Long bookingId,
            HttpServletRequest request
    ) {
        Long userId = extractUserId(request);
        ReceiptDto receipt = receiptService.getReceiptByBooking(bookingId, userId);
        return ResponseEntity.ok(receipt);
    }

    @GetMapping("/download/{receiptId}")
    public ResponseEntity<byte[]> downloadReceipt(
            @PathVariable Long receiptId,
            HttpServletRequest request
    ) {
        Long userId = extractUserId(request);
        byte[] receiptData = receiptService.downloadReceiptPdf(receiptId, userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "receipt-" + receiptId + ".txt");

        return ResponseEntity.ok()
                .headers(headers)
                .body(receiptData);
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