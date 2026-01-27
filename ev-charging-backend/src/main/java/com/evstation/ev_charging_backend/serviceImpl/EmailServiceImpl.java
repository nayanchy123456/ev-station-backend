package com.evstation.ev_charging_backend.serviceImpl;

import com.evstation.ev_charging_backend.entity.Booking;
import com.evstation.ev_charging_backend.entity.Payment;
import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:EV Charging Station}")
    private String appName;

    @Override
    public void sendBookingConfirmation(User user, Booking booking, Payment payment) {
        try {
            String subject = "Booking Confirmed - " + booking.getCharger().getName();
            String htmlContent = buildBookingConfirmationEmail(user, booking, payment);
            sendEmail(user.getEmail(), subject, htmlContent);
            logger.info("Booking confirmation email sent to {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send booking confirmation email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Override
    public void sendPaymentSuccess(User user, Payment payment, Booking booking) {
        try {
            String subject = "Payment Successful - Booking #" + booking.getId();
            String htmlContent = buildPaymentSuccessEmail(user, payment, booking);
            sendEmail(user.getEmail(), subject, htmlContent);
            logger.info("Payment success email sent to {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send payment success email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Override
    public void sendPaymentFailure(User user, Payment payment, String reason) {
        try {
            String subject = "Payment Failed - Please Try Again";
            String htmlContent = buildPaymentFailureEmail(user, payment, reason);
            sendEmail(user.getEmail(), subject, htmlContent);
            logger.info("Payment failure email sent to {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send payment failure email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Override
    public void sendCancellationConfirmation(User user, Booking booking, Payment payment) {
        try {
            String subject = "Booking Cancelled - " + booking.getCharger().getName();
            String htmlContent = buildCancellationEmail(user, booking, payment);
            sendEmail(user.getEmail(), subject, htmlContent);
            logger.info("Cancellation confirmation email sent to {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send cancellation email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Override
    public void sendHostBookingNotification(User host, Booking booking) {
        try {
            String subject = "New Booking Received - " + booking.getCharger().getName();
            String htmlContent = buildHostNotificationEmail(host, booking);
            sendEmail(host.getEmail(), subject, htmlContent);
            logger.info("Host notification email sent to {}", host.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send host notification email to {}: {}", host.getEmail(), e.getMessage());
        }
    }

    @Override
    public void sendReceiptEmail(User user, Booking booking, Payment payment) {
        try {
            String subject = "Receipt - Booking #" + booking.getId();
            String htmlContent = buildReceiptEmail(user, booking, payment);
            sendEmail(user.getEmail(), subject, htmlContent);
            logger.info("Receipt email sent to {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send receipt email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String buildBookingConfirmationEmail(User user, Booking booking, Payment payment) {
        String userName = getUserName(user);
        long durationMinutes = Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 20px; margin-top: 20px; }
                    .booking-details { background-color: white; padding: 15px; margin: 15px 0; border-left: 4px solid #4CAF50; }
                    .detail-row { margin: 10px 0; }
                    .label { font-weight: bold; color: #555; }
                    .value { color: #333; }
                    .footer { text-align: center; margin-top: 30px; color: #777; font-size: 12px; }
                    .button { background-color: #4CAF50; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Booking Confirmed!</h1>
                    </div>
                    
                    <div class="content">
                        <p>Dear %s,</p>
                        
                        <p>Your booking has been confirmed! We're excited to serve you.</p>
                        
                        <div class="booking-details">
                            <h3>📋 Booking Details</h3>
                            
                            <div class="detail-row">
                                <span class="label">Booking ID:</span>
                                <span class="value">#%d</span>
                            </div>
                            
                            <div class="detail-row">
                                <span class="label">Charger:</span>
                                <span class="value">%s</span>
                            </div>
                            
                            <div class="detail-row">
                                <span class="label">Location:</span>
                                <span class="value">%s</span>
                            </div>
                            
                            <div class="detail-row">
                                <span class="label">Start Time:</span>
                                <span class="value">%s</span>
                            </div>
                            
                            <div class="detail-row">
                                <span class="label">End Time:</span>
                                <span class="value">%s</span>
                            </div>
                            
                            <div class="detail-row">
                                <span class="label">Duration:</span>
                                <span class="value">%d minutes</span>
                            </div>
                            
                            <div class="detail-row">
                                <span class="label">Amount Paid:</span>
                                <span class="value">NPR %.2f</span>
                            </div>
                            
                            <div class="detail-row">
                                <span class="label">Transaction ID:</span>
                                <span class="value">%s</span>
                            </div>
                        </div>
                        
                        <p><strong>⚡ Your charging session will start automatically at the scheduled time.</strong></p>
                        
                        <p>If you need to cancel, please do so at least 1 hour before your scheduled time.</p>
                    </div>
                    
                    <div class="footer">
                        <p>Thank you for choosing %s!</p>
                        <p>Need help? Contact us at %s</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            userName,
            booking.getId(),
            booking.getCharger().getName(),
            booking.getCharger().getLocation(),
            booking.getStartTime().format(formatter),
            booking.getEndTime().format(formatter),
            durationMinutes,
            payment.getAmount(),
            payment.getTransactionId(),
            appName,
            fromEmail
        );
    }

    private String buildPaymentSuccessEmail(User user, Payment payment, Booking booking) {
        String userName = getUserName(user);

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 20px; margin-top: 20px; }
                    .success-box { background-color: #d4edda; border: 1px solid #c3e6cb; color: #155724; padding: 15px; border-radius: 5px; margin: 15px 0; }
                    .detail-row { margin: 10px 0; }
                    .label { font-weight: bold; color: #555; }
                    .value { color: #333; }
                    .footer { text-align: center; margin-top: 30px; color: #777; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>✅ Payment Successful!</h1>
                    </div>
                    
                    <div class="content">
                        <p>Dear %s,</p>
                        
                        <div class="success-box">
                            <h3>💰 Payment Confirmed</h3>
                            <p>Your payment of <strong>NPR %.2f</strong> has been successfully processed.</p>
                        </div>
                        
                        <div class="detail-row">
                            <span class="label">Transaction ID:</span>
                            <span class="value">%s</span>
                        </div>
                        
                        <div class="detail-row">
                            <span class="label">Payment Method:</span>
                            <span class="value">%s</span>
                        </div>
                        
                        <div class="detail-row">
                            <span class="label">Date:</span>
                            <span class="value">%s</span>
                        </div>
                        
                        <div class="detail-row">
                            <span class="label">Booking ID:</span>
                            <span class="value">#%d</span>
                        </div>
                        
                        <p>Your receipt has been attached to this email.</p>
                    </div>
                    
                    <div class="footer">
                        <p>Thank you for your payment!</p>
                        <p>%s</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            userName,
            payment.getAmount(),
            payment.getTransactionId(),
            payment.getPaymentMethod(),
            payment.getCompletedAt().format(formatter),
            booking.getId(),
            appName
        );
    }

    private String buildPaymentFailureEmail(User user, Payment payment, String reason) {
        String userName = getUserName(user);

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #f44336; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 20px; margin-top: 20px; }
                    .error-box { background-color: #f8d7da; border: 1px solid #f5c6cb; color: #721c24; padding: 15px; border-radius: 5px; margin: 15px 0; }
                    .footer { text-align: center; margin-top: 30px; color: #777; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>❌ Payment Failed</h1>
                    </div>
                    
                    <div class="content">
                        <p>Dear %s,</p>
                        
                        <div class="error-box">
                            <h3>Payment Unsuccessful</h3>
                            <p><strong>Reason:</strong> %s</p>
                        </div>
                        
                        <p>Your booking reservation has been cancelled. Please try booking again.</p>
                        
                        <p>If you continue to experience issues, please contact our support team.</p>
                    </div>
                    
                    <div class="footer">
                        <p>Need help? Contact us at %s</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            userName,
            reason,
            fromEmail
        );
    }

    private String buildCancellationEmail(User user, Booking booking, Payment payment) {
        String userName = getUserName(user);

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #FF9800; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 20px; margin-top: 20px; }
                    .info-box { background-color: #fff3cd; border: 1px solid #ffeaa7; color: #856404; padding: 15px; border-radius: 5px; margin: 15px 0; }
                    .footer { text-align: center; margin-top: 30px; color: #777; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔄 Booking Cancelled</h1>
                    </div>
                    
                    <div class="content">
                        <p>Dear %s,</p>
                        
                        <div class="info-box">
                            <h3>Your booking has been cancelled</h3>
                            <p>Booking ID: <strong>#%d</strong></p>
                            <p>Charger: <strong>%s</strong></p>
                        </div>
                        
                        <p><strong>Refund Details:</strong></p>
                        <ul>
                            <li>Amount: NPR %.2f</li>
                            <li>Refund ID: %s</li>
                            <li>Status: Processed</li>
                        </ul>
                        
                        <p>Your refund will be credited to your original payment method within 5-7 business days.</p>
                    </div>
                    
                    <div class="footer">
                        <p>We hope to serve you again soon!</p>
                        <p>%s</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            userName,
            booking.getId(),
            booking.getCharger().getName(),
            payment.getAmount(),
            payment.getRefundId(),
            appName
        );
    }

    private String buildHostNotificationEmail(User host, Booking booking) {
        String hostName = getUserName(host);
        String userName = getUserName(booking.getUser());

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #2196F3; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 20px; margin-top: 20px; }
                    .booking-details { background-color: white; padding: 15px; margin: 15px 0; border-left: 4px solid #2196F3; }
                    .footer { text-align: center; margin-top: 30px; color: #777; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔔 New Booking Received!</h1>
                    </div>
                    
                    <div class="content">
                        <p>Dear %s,</p>
                        
                        <p>You have received a new booking for your charger!</p>
                        
                        <div class="booking-details">
                            <h3>Booking Information</h3>
                            <p><strong>Customer:</strong> %s</p>
                            <p><strong>Charger:</strong> %s</p>
                            <p><strong>Start Time:</strong> %s</p>
                            <p><strong>End Time:</strong> %s</p>
                            <p><strong>Amount:</strong> NPR %.2f</p>
                        </div>
                        
                        <p>Please ensure your charger is ready for the scheduled time.</p>
                    </div>
                    
                    <div class="footer">
                        <p>%s - Host Dashboard</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            hostName,
            userName,
            booking.getCharger().getName(),
            booking.getStartTime().format(formatter),
            booking.getEndTime().format(formatter),
            booking.getTotalPrice(),
            appName
        );
    }

    private String buildReceiptEmail(User user, Booking booking, Payment payment) {
        String userName = getUserName(user);
        long durationMinutes = Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #673AB7; color: white; padding: 20px; text-align: center; }
                    .receipt { background-color: white; padding: 20px; margin: 20px 0; border: 2px solid #673AB7; }
                    .receipt-header { border-bottom: 2px solid #673AB7; padding-bottom: 10px; margin-bottom: 15px; }
                    .detail-row { display: flex; justify-content: space-between; margin: 8px 0; }
                    .total-row { border-top: 2px solid #673AB7; padding-top: 10px; margin-top: 15px; font-weight: bold; font-size: 18px; }
                    .footer { text-align: center; margin-top: 30px; color: #777; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🧾 Payment Receipt</h1>
                    </div>
                    
                    <div class="receipt">
                        <div class="receipt-header">
                            <h2>%s</h2>
                            <p>Receipt for Booking #%d</p>
                        </div>
                        
                        <div class="detail-row">
                            <span>Customer Name:</span>
                            <span>%s</span>
                        </div>
                        
                        <div class="detail-row">
                            <span>Email:</span>
                            <span>%s</span>
                        </div>
                        
                        <div class="detail-row">
                            <span>Charger:</span>
                            <span>%s</span>
                        </div>
                        
                        <div class="detail-row">
                            <span>Location:</span>
                            <span>%s</span>
                        </div>
                        
                        <div class="detail-row">
                            <span>Duration:</span>
                            <span>%d minutes</span>
                        </div>
                        
                        <div class="detail-row">
                            <span>Start Time:</span>
                            <span>%s</span>
                        </div>
                        
                        <div class="detail-row">
                            <span>End Time:</span>
                            <span>%s</span>
                        </div>
                        
                        <div class="detail-row">
                            <span>Payment Method:</span>
                            <span>%s</span>
                        </div>
                        
                        <div class="detail-row">
                            <span>Transaction ID:</span>
                            <span>%s</span>
                        </div>
                        
                        <div class="detail-row">
                            <span>Payment Date:</span>
                            <span>%s</span>
                        </div>
                        
                        <div class="total-row detail-row">
                            <span>Total Amount Paid:</span>
                            <span>NPR %.2f</span>
                        </div>
                    </div>
                    
                    <div class="footer">
                        <p>This is a computer-generated receipt. No signature required.</p>
                        <p>%s</p>
                        <p>Questions? Contact us at %s</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            appName,
            booking.getId(),
            userName,
            user.getEmail(),
            booking.getCharger().getName(),
            booking.getCharger().getLocation(),
            durationMinutes,
            booking.getStartTime().format(formatter),
            booking.getEndTime().format(formatter),
            payment.getPaymentMethod(),
            payment.getTransactionId(),
            payment.getCompletedAt().format(formatter),
            payment.getAmount(),
            appName,
            fromEmail
        );
    }

    private String getUserName(User user) {
        if (user.getFirstName() != null && user.getLastName() != null) {
            return user.getFirstName() + " " + user.getLastName();
        }
        return user.getEmail();
    }
}