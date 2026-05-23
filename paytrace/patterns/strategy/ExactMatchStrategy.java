package com.paytrace.patterns.strategy;

import com.paytrace.models.Invoice;
import com.paytrace.models.Payment;
import com.paytrace.models.ReconciliationMatch;
import com.paytrace.models.UserConnection;
import com.paytrace.models.enums.MatchType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exact Match Strategy (UC-04 / UC-05).
 *
 * Strictest matcher — runs first. Matches a payment to an invoice when:
 *   - the payment's sender_account is registered to a known user (vendor reference)
 *   - the amount equals the invoice's remaining balance EXACTLY (zero tolerance)
 *   - the payment date equals the invoice's created date EXACTLY (zero days)
 *
 * Mirrors the design-document UC-04 strategy 1: "Engine finds payments with
 * identical amount, vendor reference, and date."
 */
public class ExactMatchStrategy extends MatchingStrategy {

    @Override
    public String getStrategyName() {
        return "Exact Match Strategy";
    }

    @Override
    protected List<ReconciliationMatch> match(ReconciliationContext ctx) {
        List<ReconciliationMatch> results = new ArrayList<>();

        Map<String, String> accountToUserId = new HashMap<>();
        for (UserConnection uc : ctx.getUserConnections()) {
            if (uc.getUserAccountNumber() != null) {
                accountToUserId.put(uc.getUserAccountNumber().trim(), uc.getUserId());
            }
        }

        for (Payment p : ctx.getPayments()) {
            if (p.getStatus() != null && p.getStatus().name().equals("FULLY_ALLOCATED")) continue;
            if (p.getSenderAccount() == null || p.getSenderAccount().isBlank()) continue;
            if (p.getPaymentDate() == null) continue;

            String userId = accountToUserId.get(p.getSenderAccount().trim());
            if (userId == null) continue;

            for (Invoice inv : ctx.getInvoices()) {
                if (!userId.equals(inv.getUserId())) continue;
                if (inv.getRemainingBalance() == null) continue;
                if (inv.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) continue;
                if (inv.getCreatedAt() == null) continue;

                // Strictest: amount equal AND date equal — both exact, no tolerance.
                boolean amountExact = inv.getRemainingBalance().compareTo(p.getAmountPaid()) == 0;
                boolean dateExact   = inv.getCreatedAt().toLocalDate()
                                          .equals(p.getPaymentDate());

                if (amountExact && dateExact) {
                    ctx.log("  ✓✓ TXN-" + p.getTransactionId()
                            + " (" + p.getAmountPaid() + ") ↔ "
                            + inv.getInvoiceNumber()
                            + " — EXACT (amount + account + date all identical)");
                    ReconciliationMatch m = new ReconciliationMatch();
                    m.setInvoiceId(inv.getInvoiceId());
                    m.setPaymentId(p.getPaymentId());
                    m.setMatchType(MatchType.EXACT);
                    m.setConfidenceScore(1.0);
                    m.setExplanation("Exact match: amount, vendor reference, and date all identical");
                    m.setReversed(false);
                    m.setMatchedAt(LocalDateTime.now());
                    m.setAmountDifference(BigDecimal.ZERO);
                    results.add(m);
                    break;
                }
            }
        }
        return results;
    }
}
