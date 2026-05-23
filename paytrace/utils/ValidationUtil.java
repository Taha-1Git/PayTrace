package com.paytrace.utils;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Reusable validation rules used across all forms.
 * Each method throws ValidationException on failure with a clear message.
 */
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public static class ValidationException extends RuntimeException {
        public ValidationException(String msg) { super(msg); }
    }

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " is required.");
        }
    }

    public static void requireEmail(String value, String fieldName) {
        requireNonBlank(value, fieldName);
        if (!EMAIL_PATTERN.matcher(value.trim()).matches()) {
            throw new ValidationException(fieldName + " must be a valid email address.");
        }
    }

    public static void requireMinLength(String value, int min, String fieldName) {
        requireNonBlank(value, fieldName);
        if (value.length() < min) {
            throw new ValidationException(
                    fieldName + " must be at least " + min + " characters.");
        }
    }

    public static void requireMaxLength(String value, int max, String fieldName) {
        if (value != null && value.length() > max) {
            throw new ValidationException(
                    fieldName + " cannot exceed " + max + " characters.");
        }
    }

    public static BigDecimal requirePositiveAmount(String value, String fieldName) {
        requireNonBlank(value, fieldName);
        try {
            BigDecimal amt = new BigDecimal(value.trim());
            if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException(fieldName + " must be greater than zero.");
            }
            return amt;
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a valid number.");
        }
    }

    public static int requirePositiveInt(String value, String fieldName) {
        requireNonBlank(value, fieldName);
        try {
            int n = Integer.parseInt(value.trim());
            if (n < 0) throw new ValidationException(fieldName + " cannot be negative.");
            return n;
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a whole number.");
        }
    }

    public static void requireExactLength(String value, int len, String fieldName) {
        requireNonBlank(value, fieldName);
        if (value.length() != len) {
            throw new ValidationException(
                    fieldName + " must be exactly " + len + " characters.");
        }
    }

    public static void requireDigitsOnly(String value, String fieldName) {
        requireNonBlank(value, fieldName);
        if (!value.matches("\\d+")) {
            throw new ValidationException(fieldName + " must contain digits only.");
        }
    }
}