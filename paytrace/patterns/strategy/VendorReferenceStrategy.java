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
 * Vendor Reference Match Strategy (UC-04 / UC-05).
 *
 * Matches a payment to an invoice by the user's vendor reference
 * (the registered sender_account number) plus amount within tolerance,
 * regardless of the date.
 *
 * Mirrors the design-document UC-04 strategy 3: "Engine applies fuzzy
 * matching on vendor name fields. Matches at or above 85% confidence
 * recorded as Matched -- Fuzzy."
 *
 * In our implementation the user's account_number IS the vendor reference,
 * so the match is high-confidence (1.0) when accounts equal exactly.
 */
public class VendorReferenceStrategy extends MatchingStrategy {

    @Override
    public String getStrategyName() {
        return "Vendor Reference Match Strategy";
    }

    @Override
    protected List<ReconciliationMatch> match(ReconciliationContext ctx) {
        List<ReconciliationMatch> results = new ArrayList<>();

        // Build a quick lookup: account_number -> user_id
        Map<String, String> accountToUserId = new HashMap<>();
        for (UserConnection uc : ctx.getUserConnections()) {
            if (uc.getUserAccountNumber() != null) {
                accountToUserId.put(uc.getUserAccountNumber().trim(), uc.getUserId());
            }
        }

        for (Payment p : ctx.getPayments()) {
            if (p.getStatus() != null && p.getStatus().name().equals("FULLY_ALLOCATED")) continue;
            if (p.getSenderAccount() == null || p.getSenderAccount().isBlank()) {
                ctx.log("  ⏭ Skip TXN-" + p.getTransactionId() + " — no sender account");
                continue;
            }

            String userId = accountToUserId.get(p.getSenderAccount().trim());
            if (userId == null) {
                ctx.log("  ✗ TXN-" + p.getTransactionId()
                        + " from " + p.getSenderAccount() + " — account not registered");
                continue;
            }

            // Find an invoice for this user with a matching remaining balance
            Invoice matched = null;
            for (Invoice inv : ctx.getInvoices()) {
                if (!userId.equals(inv.getUserId())) continue;
                if (inv.getRemainingBalance() == null) continue;
                if (inv.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal diff = inv.getRemainingBalance().subtract(p.getAmountPaid()).abs();
                if (diff.compareTo(ctx.getAmountTolerance()) <= 0) {
                    matched = inv;
                    break;
                }
            }

            if (matched != null) {
                ctx.log("  ✓ TXN-" + p.getTransactionId()
                        + " (" + p.getAmountPaid() + ") ↔ "
                        + matched.getInvoiceNumber() + " — vendor reference + amount MATCH");
                results.add(buildMatch(matched, p,
                        "Vendor reference (account number) + amount match"));
            } else {
                ctx.log("  ✗ TXN-" + p.getTransactionId()
                        + " from registered account " + p.getSenderAccount()
                        + " — no invoice with matching balance");
            }
        }
        return results;
    }

    private ReconciliationMatch buildMatch(Invoice inv, Payment p, String explanation) {
        ReconciliationMatch m = new ReconciliationMatch();
        m.setInvoiceId(inv.getInvoiceId());
        m.setPaymentId(p.getPaymentId());
        m.setMatchType(MatchType.EXACT);
        m.setConfidenceScore(1.0);
        m.setExplanation(explanation);
        m.setReversed(false);
        m.setMatchedAt(LocalDateTime.now());
        m.setAmountDifference(BigDecimal.ZERO);
        return m;
    }
}
