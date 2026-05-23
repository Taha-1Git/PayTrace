package com.paytrace.models.enums;

public enum PaymentStatus {
    UNMATCHED,
    FULLY_ALLOCATED,
    PARTIALLY_ALLOCATED,
    MATCHED,
    PARTIALLY_MATCHED,
    OVERPAID,
    REFUNDED_OVERPAY,
    REJECTED,
    MANUAL_REVIEW
}