package com.evstation.ev_charging_backend.util;

import org.springframework.stereotype.Component;

/**
 * Utility class for validating Nepal phone numbers.
 * Nepal country code: +977
 * Local number should be exactly 10 digits (starting with 9 or 8 for mobile)
 */
@Component
public class PhoneValidator {

    // Nepal country code
    public static final String NEPAL_COUNTRY_CODE = "+977";
    public static final String NEPAL_CODE_WITHOUT_PLUS = "977";
    
    // Phone number should be exactly 10 digits
    public static final int NEPAL_PHONE_LENGTH = 10;
    
    // Valid starting digits for Nepal mobile numbers (9 or 8)
    public static final String VALID_STARTING_DIGITS = "98";

    /**
     * Validates a phone number for Nepal.
     * Accepts formats:
     * - 9XXXXXXXXX (10 digits starting with 9)
     * - 8XXXXXXXXX (10 digits starting with 8)
     * - +9779XXXXXXXXX (with country code)
     * - 9779XXXXXXXXX (country code without +)
     * 
     * @param phone the phone number to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidNepalPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }

        String cleanedPhone = cleanPhoneNumber(phone);
        
        // Check if it's exactly 10 digits
        if (cleanedPhone.length() != NEPAL_PHONE_LENGTH) {
            return false;
        }

        // Check if all characters are digits
        if (!cleanedPhone.matches("\\d+")) {
            return false;
        }

        // Check if it starts with 9 or 8 (valid for Nepal mobile)
        String firstDigit = cleanedPhone.substring(0, 1);
        return VALID_STARTING_DIGITS.contains(firstDigit);
    }

    /**
     * Cleans the phone number by removing country code if present.
     * Returns only the local 10-digit number.
     * 
     * @param phone the raw phone number
     * @return cleaned phone number (10 digits only)
     */
    public static String cleanPhoneNumber(String phone) {
        if (phone == null) {
            return "";
        }

        String cleaned = phone.trim();

        // Remove + if present
        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }

        // Remove country code if present
        if (cleaned.startsWith(NEPAL_CODE_WITHOUT_PLUS)) {
            cleaned = cleaned.substring(3);
        }

        // Remove any spaces or hyphens
        cleaned = cleaned.replaceAll("[\\s\\-]", "");

        return cleaned;
    }

    /**
     * Formats a phone number to include Nepal country code.
     * Input can be just 10 digits or with country code.
     * Output: +977XXXXXXXXXX
     * 
     * @param phone the phone number to format
     * @return formatted phone number with country code, or empty string if invalid
     */
    public static String formatPhoneWithCountryCode(String phone) {
        if (!isValidNepalPhone(phone)) {
            return "";
        }

        String cleaned = cleanPhoneNumber(phone);
        return NEPAL_COUNTRY_CODE + cleaned;
    }

    /**
     * Returns error message for invalid phone format.
     * 
     * @return error message
     */
    public static String getErrorMessage() {
        return "Phone number must be a valid Nepal number with exactly 10 digits (starting with 8 or 9)";
    }
}
