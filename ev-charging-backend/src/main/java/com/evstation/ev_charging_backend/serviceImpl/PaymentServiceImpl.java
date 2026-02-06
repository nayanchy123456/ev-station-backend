package com.evstation.ev_charging_backend.serviceImpl;

import com.evstation.ev_charging_backend.dto.*;
import com.evstation.ev_charging_backend.entity.Booking;
import com.evstation.ev_charging_backend.entity.Payment;
import com.evstation.ev_charging_backend.enums.BookingStatus;
import com.evstation.ev_charging_backend.enums.NotificationType;
import com.evstation.ev_charging_backend.enums.PaymentStatus;
import com.evstation.ev_charging_backend.exception.*;
import com.evstation.ev_charging_backend.repository.BookingRepository;
import com.evstation.ev_charging_backend.repository.PaymentRepository;
import com.evstation.ev_charging_backend.service.EmailService;
import com.evstation.ev_charging_backend.service.NotificationService;
import com.evstation.ev_charging_backend.service.PaymentService;
import com.evstation.ev_charging_backend.service.ReceiptService;
import com.evstation.ev_charging_backend.service.ReservationService;
import com.evstation.ev_charging_backend.util.MockPaymentUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * ✅ ENHANCED PaymentServiceImpl with Better Logging
 * 
 * CHANGES:
 * 1. Added comprehensive logging for debugging
 * 2. Logs host ID when status changes
 * 3. Maintains all existing functionality
 */
@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final ReservationService reservationService;
    private final NotificationService notificationService;
    private final ReceiptService receiptService;
    private final MockPaymentUtil mockPaymentUtil;
    private final EmailService emailService;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            ReservationService reservationService,
            NotificationService notificationService,
            ReceiptService receiptService,
            MockPaymentUtil mockPaymentUtil,
            EmailService emailService
    ) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.reservationService = reservationService;
        this.notificationService = notificationService;
        this.receiptService = receiptService;
        this.mockPaymentUtil = mockPaymentUtil;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public PaymentInitiateResponseDto initiatePayment(PaymentInitiateRequestDto dto, Long userId) {
        log.info("💳 User {} initiating payment for reservation {}", userId, dto.getReservationId());
        
        // Validate reservation
        reservationService.validateReservation(dto.getReservationId(), userId);

        Booking booking = bookingRepository.findById(dto.getReservationId())
                .orElseThrow(() -> new BookingNotFoundException("Reservation not found"));

        // ✅ LOG CURRENT STATUS AND HOST
        Long hostId = booking.getCharger().getHost().getUserId();
        log.info("📋 Booking #{} current status: {} (host: {})", 
                 booking.getId(), booking.getStatus(), hostId);

        // Check if payment already exists
        Optional<Payment> existingPayment = paymentRepository.findByBookingId(booking.getId());
        
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();
            
            if (payment.getStatus() == PaymentStatus.PENDING) {
                if (booking.getReservedUntil() != null && 
                    LocalDateTime.now().isAfter(booking.getReservedUntil())) {
                    throw new ReservationExpiredException("Reservation has expired. Please create a new booking.");
                }
                
                log.info("♻️ Returning existing PENDING payment {} for booking {}", 
                         payment.getId(), booking.getId());
                
                return PaymentInitiateResponseDto.builder()
                        .paymentId(payment.getId())
                        .bookingId(booking.getId())
                        .amount(payment.getAmount())
                        .currency(payment.getCurrency())
                        .status(PaymentStatus.PENDING)
                        .paymentMethod(payment.getPaymentMethod())
                        .expiresAt(booking.getReservedUntil())
                        .message("Payment is pending. You can proceed to complete the payment.")
                        .build();
            }
            
            throw new IllegalStateException("Payment already " + payment.getStatus().toString().toLowerCase() + " for this booking");
        }

        // Calculate amount
        long durationMinutes = Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();
        double estimatedKwh = (durationMinutes / 60.0) * 7.0;
        BigDecimal amount = booking.getPricePerKwh()
                .multiply(BigDecimal.valueOf(estimatedKwh))
                .setScale(2, BigDecimal.ROUND_HALF_UP);

        // Create payment record
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(amount)
                .currency("NPR")
                .status(PaymentStatus.PENDING)
                .paymentMethod(dto.getPaymentMethod())
                .remarks(dto.getRemarks())
                .build();

        paymentRepository.save(payment);

        // ✅ ENHANCED: Update booking status to PAYMENT_PENDING
        BookingStatus previousStatus = booking.getStatus();
        booking.setStatus(BookingStatus.PAYMENT_PENDING);
        booking.setTotalPrice(amount);
        bookingRepository.save(booking);

        // ✅ ENHANCED LOGGING
        log.info("✅ Payment #{} initiated for booking #{}", payment.getId(), booking.getId());
        log.info("📊 Booking #{} status: {} → PAYMENT_PENDING (host: {})", 
                 booking.getId(), previousStatus, hostId);
        log.info("💰 Payment amount: NPR {}", amount);

        return PaymentInitiateResponseDto.builder()
                .paymentId(payment.getId())
                .bookingId(booking.getId())
                .amount(amount)
                .currency("NPR")
                .status(PaymentStatus.PENDING)
                .paymentMethod(dto.getPaymentMethod())
                .expiresAt(booking.getReservedUntil())
                .message("Payment initiated successfully. Complete payment within the reservation window.")
                .build();
    }

    @Override
    @Transactional
    public PaymentProcessResponseDto processPayment(PaymentProcessRequestDto dto, Long userId) {
        log.info("💳 User {} processing payment {}", userId, dto.getPaymentId());
        
        Payment payment = paymentRepository.findById(dto.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        Booking booking = payment.getBooking();
        Long hostId = booking.getCharger().getHost().getUserId();

        if (!booking.getUser().getUserId().equals(userId)) {
            throw new SecurityException("You are not allowed to process this payment");
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentStatusException("Payment is not in pending state");
        }

        // Check if reservation has expired
        if (booking.getReservedUntil() != null && 
            LocalDateTime.now().isAfter(booking.getReservedUntil())) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Reservation expired");
            payment.setCompletedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);

            log.warn("⏰ Payment {} failed - reservation expired (host: {})", 
                     payment.getId(), hostId);

            throw new ReservationExpiredException("Reservation has expired. Please create a new booking.");
        }

        // Simulate payment processing
        mockPaymentUtil.simulatePaymentDelay();

        boolean paymentSuccess = dto.getConfirmPayment() && mockPaymentUtil.determinePaymentOutcome();

        if (paymentSuccess) {
            // Payment successful
            String transactionId = mockPaymentUtil.generateTransactionId();
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(transactionId);
            payment.setCompletedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // ✅ ENHANCED: Update booking status to CONFIRMED
            BookingStatus previousStatus = booking.getStatus();
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            // ✅ ENHANCED LOGGING
            log.info("✅ Payment {} successful for booking #{}", payment.getId(), booking.getId());
            log.info("📊 Booking #{} status: {} → CONFIRMED (host: {})", 
                     booking.getId(), previousStatus, hostId);
            log.info("🎫 Transaction ID: {}", transactionId);

            // Generate receipt
            ReceiptDto receipt = receiptService.generateReceipt(booking, payment);

            // Send notifications
            notificationService.notifyUser(userId, NotificationType.BOOKING_CONFIRMED, booking);
            notificationService.notifyHost(hostId, NotificationType.BOOKING_CONFIRMED, booking);
            
            log.info("🔔 Notifications sent to user {} and host {}", userId, hostId);

            // Send emails
            emailService.sendReceiptEmail(booking.getUser(), booking, payment);
            emailService.sendBookingConfirmation(booking.getUser(), booking, payment);

            return PaymentProcessResponseDto.builder()
                    .success(true)
                    .paymentId(payment.getId())
                    .transactionId(transactionId)
                    .status(PaymentStatus.SUCCESS)
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .completedAt(payment.getCompletedAt())
                    .receipt(receipt)
                    .message("Payment successful! Your booking is confirmed.")
                    .build();
        } else {
            // Payment failed
            String failureReason = mockPaymentUtil.generateMockFailureReason();
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(failureReason);
            payment.setCompletedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            // ✅ ENHANCED LOGGING
            log.warn("❌ Payment {} failed for booking #{} (host: {})", 
                     payment.getId(), booking.getId(), hostId);
            log.warn("❌ Failure reason: {}", failureReason);
            log.info("📊 Booking #{} status: PAYMENT_PENDING → CANCELLED", booking.getId());

            // Send failure notification
            notificationService.notifyUser(userId, NotificationType.PAYMENT_FAILED, booking);
            emailService.sendPaymentFailure(booking.getUser(), payment, failureReason);

            throw new PaymentFailedException("Payment failed: " + failureReason);
        }
    }

    @Override
    @Transactional
    public CancellationResponseDto processRefund(Long paymentId, Long userId, String reason) {
        log.info("💸 User {} requesting refund for payment {}", userId, paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        Booking booking = payment.getBooking();
        Long hostId = booking.getCharger().getHost().getUserId();

        if (!booking.getUser().getUserId().equals(userId)) {
            throw new SecurityException("You are not allowed to refund this payment");
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new RefundNotAllowedException("Only successful payments can be refunded");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RefundNotAllowedException("Booking is not in a refundable state");
        }

        // Generate refund
        String refundId = mockPaymentUtil.generateRefundId();
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundId(refundId);
        payment.setRefundedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // ✅ ENHANCED LOGGING
        log.info("✅ Refund {} processed for payment {}, booking #{}", 
                 refundId, paymentId, booking.getId());
        log.info("📊 Booking #{} status: CONFIRMED → CANCELLED (host: {})", 
                 booking.getId(), hostId);
        log.info("💰 Refund amount: NPR {}", payment.getAmount());

        // Send notifications
        notificationService.notifyUser(userId, NotificationType.REFUND_PROCESSED, booking);
        notificationService.notifyHost(hostId, NotificationType.BOOKING_CANCELLED, booking);

        emailService.sendCancellationConfirmation(booking.getUser(), booking, payment);

        return CancellationResponseDto.builder()
                .success(true)
                .bookingId(booking.getId())
                .message("Booking cancelled and refund processed successfully")
                .refundProcessed(true)
                .refundAmount(payment.getAmount())
                .refundId(refundId)
                .refundedAt(payment.getRefundedAt())
                .build();
    }

    @Override
    public PaymentProcessResponseDto getPaymentById(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (!payment.getBooking().getUser().getUserId().equals(userId)) {
            throw new SecurityException("You are not allowed to view this payment");
        }

        return mapToPaymentResponse(payment);
    }

    @Override
    public PaymentProcessResponseDto getPaymentByBooking(Long bookingId, Long userId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for this booking"));

        if (!payment.getBooking().getUser().getUserId().equals(userId)) {
            throw new SecurityException("You are not allowed to view this payment");
        }

        return mapToPaymentResponse(payment);
    }

    private PaymentProcessResponseDto mapToPaymentResponse(Payment payment) {
        ReceiptDto receipt = null;
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            try {
                receipt = receiptService.getReceiptByBooking(payment.getBooking().getId(), 
                    payment.getBooking().getUser().getUserId());
            } catch (Exception e) {
                // Receipt not generated yet
            }
        }

        return PaymentProcessResponseDto.builder()
                .success(payment.getStatus() == PaymentStatus.SUCCESS)
                .paymentId(payment.getId())
                .transactionId(payment.getTransactionId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .failureReason(payment.getFailureReason())
                .completedAt(payment.getCompletedAt())
                .receipt(receipt)
                .message(getStatusMessage(payment.getStatus()))
                .build();
    }

    private String getStatusMessage(PaymentStatus status) {
        return switch (status) {
            case PENDING -> "Payment is pending";
            case SUCCESS -> "Payment completed successfully";
            case FAILED -> "Payment failed";
            case REFUNDED -> "Payment refunded";
        };
    }
}