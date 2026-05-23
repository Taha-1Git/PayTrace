package com.paytrace.patterns.strategy;

import com.paytrace.models.Invoice;
import com.paytrace.models.Payment;
import com.paytrace.models.UserConnection;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bundles all data + configuration a matching strategy needs to do its job.
 * Passed to MatchingStrategy.execute().
 *
 * Includes a 'log' callback so strategies can stream their progress
 * to the UI in real time.
 */
public class ReconciliationContext {

    private final List<Invoice>        invoices;
    private final List<Payment>        payments;
    private final List<UserConnection> userConnections;
    private final BigDecimal           amountTolerance;     // e.g. 1.00 PKR rounding leeway
    private final int                  dateToleranceDays;   // e.g. 7 days between invoice + payment
    private final Consumer<String>     log;                  // called for every progress message

    public ReconciliationContext(List<Invoice> invoices,
                                 List<Payment> payments,
                                 List<UserConnection> userConnections,
                                 BigDecimal amountTolerance,
                                 int dateToleranceDays,
                                 Consumer<String> log) {
        this.invoices          = invoices;
        this.payments          = payments;
        this.userConnections   = userConnections;
        this.amountTolerance   = amountTolerance;
        this.dateToleranceDays = dateToleranceDays;
        this.log               = log;
    }

    public List<Invoice>        getInvoices()         { return invoices; }
    public List<Payment>        getPayments()         { return payments; }
    public List<UserConnection> getUserConnections()  { return userConnections; }
    public BigDecimal           getAmountTolerance()  { return amountTolerance; }
    public int                  getDateToleranceDays(){ return dateToleranceDays; }

    public void log(String message) {
        if (log != null) log.accept(message);
    }
}