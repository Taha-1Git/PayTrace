package com.paytrace.ui.controllers;

import com.paytrace.models.Payment;

/** Tiny static holder to pass the selected payment between screens. */
public class SplitPaymentContext {
    private static Payment selectedPayment;

    public static void setSelectedPayment(Payment p) { selectedPayment = p; }
    public static Payment getSelectedPayment()       { return selectedPayment; }
    public static void clear()                       { selectedPayment = null; }
}