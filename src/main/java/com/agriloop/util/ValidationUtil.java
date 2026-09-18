package com.agriloop.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Common input validation and structured address utilities.
 */
public class ValidationUtil {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+0-9\\-\\s()]{7,25}$");
    private static final Pattern PIN_CODE_PATTERN = Pattern.compile("^[1-9][0-9]{5}$");

    public record StructuredAddress(
        String houseNo,
        String street,
        String area,
        String city,
        String district,
        String state,
        String pinCode,
        String country
    ) {
        public String toCanonicalAddress() {
            return String.format("%s, %s, %s, %s, %s, %s - %s, %s",
                houseNo != null ? houseNo.trim() : "",
                street != null ? street.trim() : "",
                area != null ? area.trim() : "",
                city != null ? city.trim() : "",
                district != null ? district.trim() : "",
                state != null ? state.trim() : "",
                pinCode != null ? pinCode.trim() : "",
                country != null ? country.trim() : "India"
            );
        }
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    public static boolean isValidPinCode(String pin) {
        if (pin == null) return false;
        String clean = pin.trim().replace(" ", "");
        return PIN_CODE_PATTERN.matcher(clean).matches();
    }

    public static boolean isPositiveDecimal(String str) {
        if (str == null || str.isBlank()) return false;
        try {
            BigDecimal val = new BigDecimal(str.trim());
            return val.compareTo(BigDecimal.ZERO) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isNonNegativeDecimal(String str) {
        if (str == null || str.isBlank()) return false;
        try {
            BigDecimal val = new BigDecimal(str.trim());
            return val.compareTo(BigDecimal.ZERO) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates mandatory structured address fields and returns an error message if invalid.
     * Returns null if all 8 required fields are present and valid.
     */
    public static String validateStructuredAddress(
        String houseNo,
        String street,
        String area,
        String city,
        String district,
        String state,
        String pinCode,
        String country
    ) {
        if (houseNo == null || houseNo.trim().isBlank()) return "House / Building Number is required.";
        if (street == null || street.trim().isBlank()) return "Street Name is required.";
        if (area == null || area.trim().isBlank()) return "Area / Locality is required.";
        if (city == null || city.trim().isBlank()) return "City is required.";
        if (district == null || district.trim().isBlank()) return "District is required.";
        if (state == null || state.trim().isBlank()) return "State is required.";
        if (pinCode == null || pinCode.trim().isBlank()) return "PIN Code is required.";
        if (country == null || country.trim().isBlank()) return "Country is required.";

        String cleanPin = pinCode.trim().replace(" ", "");
        if (!PIN_CODE_PATTERN.matcher(cleanPin).matches()) {
            return "PIN Code must be a valid 6-digit postal code (e.g. 600069).";
        }
        return null;
    }

    /**
     * Validates that an address is a meaningful full address containing location details,
     * not merely a single-word city or incomplete string.
     */
    public static boolean isValidFullAddress(String address) {
        if (address == null) return false;
        String trimmed = address.trim();
        if (trimmed.length() < 10) return false;
        String[] tokens = trimmed.split("[,\\s]+");
        return tokens.length >= 3;
    }
}
