package com.evstation.ev_charging_backend.util;

import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;

@Component
public class MockPaymentUtil {

    private static final Random random = new Random();
    private static final double FAILURE_RATE = 0.1; // 10% failure rate

    /**
     * Generate a unique transaction ID
     */
    public String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase();
    }

    /**
     * Generate a unique refund ID
     */
    public String generateRefundId() {
        return "RFD-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase();
    }

    /**
     * Simulate payment processing delay (2-3 seconds)
     */
    public void simulatePaymentDelay() {
        try {
            int delay = 2000 + random.nextInt(1000); // 2-3 seconds
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Determine if payment should succeed or fail (90% success rate)
     */
    public boolean determinePaymentOutcome() {
        return random.nextDouble() > FAILURE_RATE;
    }

    /**
     * Generate a mock failure reason
     */
    public String generateMockFailureReason() {
        String[] reasons = {
            "Insufficient funds",
            "Payment gateway timeout",
            "Invalid payment method",
            "Transaction declined by bank",
            "Network error occurred",
            "Card expired"
        };
        return reasons[random.nextInt(reasons.length)];
    }

    /**
     * Generate a receipt number
     */
    public String generateReceiptNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "RCP-" + timestamp + "-" + randomPart;
    }
}