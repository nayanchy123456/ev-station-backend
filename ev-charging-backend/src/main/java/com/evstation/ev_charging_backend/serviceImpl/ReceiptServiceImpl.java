package com.evstation.ev_charging_backend.serviceImpl;

import com.evstation.ev_charging_backend.dto.ReceiptDto;
import com.evstation.ev_charging_backend.entity.Booking;
import com.evstation.ev_charging_backend.entity.Payment;
import com.evstation.ev_charging_backend.entity.Receipt;
import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.exception.ResourceNotFoundException;
import com.evstation.ev_charging_backend.repository.ReceiptRepository;
import com.evstation.ev_charging_backend.service.ReceiptService;
import com.evstation.ev_charging_backend.util.MockPaymentUtil;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ReceiptServiceImpl implements ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final MockPaymentUtil mockPaymentUtil;

    public ReceiptServiceImpl(
            ReceiptRepository receiptRepository,
            MockPaymentUtil mockPaymentUtil
    ) {
        this.receiptRepository = receiptRepository;
        this.mockPaymentUtil = mockPaymentUtil;
    }

    @Override
    @Transactional
    public ReceiptDto generateReceipt(Booking booking, Payment payment) {
        // Check if receipt already exists
        return receiptRepository.findByBookingId(booking.getId())
                .map(this::mapToDto)
                .orElseGet(() -> {
                    String receiptNumber = mockPaymentUtil.generateReceiptNumber();

                    Receipt receipt = Receipt.builder()
                            .booking(booking)
                            .payment(payment)
                            .receiptNumber(receiptNumber)
                            .pdfUrl(null) // Can be implemented later for actual PDF generation
                            .build();

                    receiptRepository.save(receipt);
                    return mapToDto(receipt);
                });
    }

    @Override
    public ReceiptDto getReceiptById(Long receiptId, Long userId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found"));

        if (!receipt.getBooking().getUser().getUserId().equals(userId)) {
            throw new SecurityException("You are not allowed to view this receipt");
        }

        return mapToDto(receipt);
    }

    @Override
    public ReceiptDto getReceiptByBooking(Long bookingId, Long userId) {
        Receipt receipt = receiptRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found for this booking"));

        if (!receipt.getBooking().getUser().getUserId().equals(userId)) {
            throw new SecurityException("You are not allowed to view this receipt");
        }

        return mapToDto(receipt);
    }

    @Override
    public byte[] downloadReceiptPdf(Long receiptId, Long userId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found"));

        if (!receipt.getBooking().getUser().getUserId().equals(userId)) {
            throw new SecurityException("You are not allowed to download this receipt");
        }

        // TODO: Implement actual PDF generation using a library like iText or Apache PDFBox
        // For now, return a placeholder
        String receiptContent = generateReceiptText(receipt);
        return receiptContent.getBytes();
    }

    private ReceiptDto mapToDto(Receipt receipt) {
        Booking booking = receipt.getBooking();
        Payment payment = receipt.getPayment();
        User user = booking.getUser();
        long durationMinutes = Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();

        // Build user name from available fields
        String userName = (user.getFirstName() != null && user.getLastName() != null)
            ? user.getFirstName() + " " + user.getLastName()
            : user.getEmail();

        return ReceiptDto.builder()
                .receiptId(receipt.getId())
                .receiptNumber(receipt.getReceiptNumber())
                .bookingId(booking.getId())
                .paymentId(payment.getId())
                .chargerName(booking.getCharger().getName())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .durationMinutes(durationMinutes)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .transactionId(payment.getTransactionId())
                .paymentDate(payment.getCompletedAt())
                .userName(userName)
                .userEmail(booking.getUser().getEmail())
                .generatedAt(receipt.getGeneratedAt())
                .pdfUrl(receipt.getPdfUrl())
                .build();
    }

    private String generateReceiptText(Receipt receipt) {
        ReceiptDto dto = mapToDto(receipt);
        
        return String.format("""
            ===============================================
                       PAYMENT RECEIPT
            ===============================================
            
            Receipt Number: %s
            Date: %s
            
            -----------------------------------------------
            CUSTOMER INFORMATION
            -----------------------------------------------
            Name: %s
            Email: %s
            
            -----------------------------------------------
            BOOKING DETAILS
            -----------------------------------------------
            Charger: %s
            Start Time: %s
            End Time: %s
            Duration: %d minutes
            
            -----------------------------------------------
            PAYMENT DETAILS
            -----------------------------------------------
            Amount: %s %.2f
            Payment Method: %s
            Transaction ID: %s
            Payment Date: %s
            
            ===============================================
            Thank you for using our EV Charging Service!
            ===============================================
            """,
            dto.getReceiptNumber(),
            dto.getGeneratedAt(),
            dto.getUserName(),
            dto.getUserEmail(),
            dto.getChargerName(),
            dto.getStartTime(),
            dto.getEndTime(),
            dto.getDurationMinutes(),
            dto.getCurrency(),
            dto.getAmount(),
            dto.getPaymentMethod(),
            dto.getTransactionId(),
            dto.getPaymentDate()
        );
    }
}