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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
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
        // Validate reservation
        reservationService.validateReservation(dto.getReservationId(), userId);

        Booking booking = bookingRepository.findById(dto.getReservationId())
                .orElseThrow(() -> new BookingNotFoundException("Reservation not found"));

        // Check if payment already exists
        paymentRepository.findByBookingId(booking.getId()).ifPresent(p -> {
            throw new IllegalStateException("Payment already initiated for this booking");
        });

        // Calculate amount (estimated based on duration)
        long durationMinutes = Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();
        double estimatedKwh = (durationMinutes / 60.0) * 7.0; // Assume 7 kWh per hour
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

        // Update booking status
        booking.setStatus(BookingStatus.PAYMENT_PENDING);
        booking.setTotalPrice(amount);
        bookingRepository.save(booking);

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
        Payment payment = paymentRepository.findById(dto.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        Booking booking = payment.getBooking();

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

            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            // Generate receipt
            ReceiptDto receipt = receiptService.generateReceipt(booking, payment);

            // Send notifications
            notificationService.notifyUser(userId, NotificationType.BOOKING_CONFIRMED, booking);
            notificationService.notifyHost(
                booking.getCharger().getHost().getUserId(), 
                NotificationType.BOOKING_CONFIRMED, 
                booking
            );

            // ✅ Send email with receipt (ONLY to user, NOT to host)
            emailService.sendReceiptEmail(booking.getUser(), booking, payment);
            emailService.sendBookingConfirmation(booking.getUser(), booking, payment);
            // ❌ REMOVED: emailService.sendHostBookingNotification() - Host gets dashboard notification only

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

            // Send failure notification
            notificationService.notifyUser(userId, NotificationType.PAYMENT_FAILED, booking);

            // ✅ Send failure email
            emailService.sendPaymentFailure(booking.getUser(), payment, failureReason);

            throw new PaymentFailedException("Payment failed: " + failureReason);
        }
    }

    @Override
    @Transactional
    public CancellationResponseDto processRefund(Long paymentId, Long userId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        Booking booking = payment.getBooking();

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

        // Send notifications
        notificationService.notifyUser(userId, NotificationType.REFUND_PROCESSED, booking);
        notificationService.notifyHost(
            booking.getCharger().getHost().getUserId(),
            NotificationType.BOOKING_CANCELLED,
            booking
        );

        // ✅ Send cancellation email
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