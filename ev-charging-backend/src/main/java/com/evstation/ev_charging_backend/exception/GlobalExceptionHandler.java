package com.evstation.ev_charging_backend.exception;

import com.evstation.ev_charging_backend.dto.AuthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // --- Auth-related exceptions ---
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<AuthResponse> handleUserExists(UserAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(AuthResponse.builder().message(ex.getMessage()).build());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<AuthResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthResponse.builder().message(ex.getMessage()).build());
    }

    // --- Booking-specific exceptions ---
    @ExceptionHandler(BookingConflictException.class)
    public ResponseEntity<Map<String, Object>> handleBookingConflict(BookingConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.CONFLICT.value(),
                        "error", "Booking Conflict",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleBookingNotFound(BookingNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.NOT_FOUND.value(),
                        "error", "Booking Not Found",
                        "message", ex.getMessage()
                ));
    }

    // --- Payment-specific exceptions ---
    @ExceptionHandler(ReservationExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleReservationExpired(ReservationExpiredException ex) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.GONE.value(),
                        "error", "Reservation Expired",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentFailed(PaymentFailedException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.PAYMENT_REQUIRED.value(),
                        "error", "Payment Failed",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(InvalidPaymentStatusException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPaymentStatus(InvalidPaymentStatusException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.BAD_REQUEST.value(),
                        "error", "Invalid Payment Status",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(RefundNotAllowedException.class)
    public ResponseEntity<Map<String, Object>> handleRefundNotAllowed(RefundNotAllowedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.BAD_REQUEST.value(),
                        "error", "Refund Not Allowed",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(CancellationDeadlinePassedException.class)
    public ResponseEntity<Map<String, Object>> handleCancellationDeadline(CancellationDeadlinePassedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.BAD_REQUEST.value(),
                        "error", "Cancellation Deadline Passed",
                        "message", ex.getMessage()
                ));
    }

    // --- Date Range Validation Exception ---
    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidDateRange(InvalidDateRangeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.BAD_REQUEST.value(),
                        "error", "Invalid Date Range",
                        "message", ex.getMessage()
                ));
    }

    // --- Rating-specific exceptions (NEW) ---
    @ExceptionHandler(RatingAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleRatingAlreadyExists(RatingAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.CONFLICT.value(),
                        "error", "Rating Already Exists",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(RatingNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRatingNotFound(RatingNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.NOT_FOUND.value(),
                        "error", "Rating Not Found",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(InvalidRatingException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRating(InvalidRatingException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.BAD_REQUEST.value(),
                        "error", "Invalid Rating",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(UnauthorizedRatingAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedRatingAccess(UnauthorizedRatingAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.FORBIDDEN.value(),
                        "error", "Unauthorized Access",
                        "message", ex.getMessage()
                ));
    }

    // --- Validation and state exceptions ---
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.BAD_REQUEST.value(),
                        "error", "Invalid Argument",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.BAD_REQUEST.value(),
                        "error", "Invalid State",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.FORBIDDEN.value(),
                        "error", "Security Error",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.FORBIDDEN.value(),
                        "error", "Access Denied",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.NOT_FOUND.value(),
                        "error", "Not Found",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                errors.put(err.getField(), err.getDefaultMessage()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.BAD_REQUEST.value(),
                        "error", "Validation Failed",
                        "details", errors
                ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "error", "Runtime Error",
                        "message", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred"
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "error", "Internal Server Error",
                        "message", ex.getMessage()
                ));
    }


    
}