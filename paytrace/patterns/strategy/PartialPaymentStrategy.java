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
 * Matches a payment to an invoice when the payment is LESS than the invoice's
 * remaining balance (i.e. user is paying partial). Reduces invoice balance,
 * leaves invoice OPEN for further partial payments.
 *
 * Runs after Account + Date strategies have claimed exact matches.
 */
public class PartialPaymentStrategy extends MatchingStrategy {

    @Override
    public String getStrategyName() {
        return "Partial Payment Strategy";
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
            if (p.getSenderAccount() == null) continue;

            String userId = accountToUserId.get(p.getSenderAccount().trim());
            if (userId == null) continue;

            Invoice candidate = null;
            for (Invoice inv : ctx.getInvoices()) {
                if (!userId.equals(inv.getUserId())) continue;
                if (inv.getRemainingBalance() == null) continue;
                if (inv.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) continue;
                if (p.getAmountPaid().compareTo(inv.getRemainingBalance()) >= 0) continue;

                // Found a partial-pay candidate
                candidate = inv;
                break;
            }

            if (candidate != null) {
                BigDecimal newRemaining =
                        candidate.getRemainingBalance().subtract(p.getAmountPaid());
                ctx.log("  ◐ TXN-" + p.getTransactionId() + " (" + p.getAmountPaid()
                        + ") ↔ " + candidate.getInvoiceNumber()
                        + " — PARTIAL: remaining drops from "
                        + candidate.getRemainingBalance() + " to " + newRemaining);

                ReconciliationMatch m = new ReconciliationMatch();
                m.setInvoiceId(candidate.getInvoiceId());
                m.setPaymentId(p.getPaymentId());
                m.setMatchType(MatchType.PARTIAL);
                m.setConfidenceScore(0.95);
                m.setExplanation("Partial payment — " + p.getAmountPaid()
                        + " of " + candidate.getRemainingBalance() + " remaining");
                m.setReversed(false);
                m.setMatchedAt(LocalDateTime.now());
                m.setAmountDifference(newRemaining);
                results.add(m);
            }
        }
        return results;
    }
}