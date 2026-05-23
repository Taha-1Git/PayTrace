package com.paytrace.patterns.strategy;

import com.paytrace.models.Invoice;
import com.paytrace.models.Payment;
import com.paytrace.models.ReconciliationMatch;
import com.paytrace.models.UserConnection;
import com.paytrace.models.enums.MatchType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tolerant Amount + Date Match Strategy (UC-04 / UC-05).
 *
 * Matches a payment to an invoice when:
 *   - sender_account is registered to a known user (vendor reference present)
 *   - amount matches within the configured tolerance window
 *   - payment_date is within ±N days of invoice's created date
 *
 * Mirrors the design-document UC-04 strategy 2: "Tolerant Amount and Date
 * Match — engine re-scans remaining unmatched invoices and finds payments
 * within configured tolerance windows."
 */
public class TolerantMatchStrategy extends MatchingStrategy {

    @Override
    public String getStrategyName() {
        return "Tolerant Amount + Date Match Strategy";
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
            if (p.getSenderAccount() == null || p.getPaymentDate() == null) continue;

            String userId = accountToUserId.get(p.getSenderAccount().trim());
            if (userId == null) continue;

            Invoice candidate = null;
            for (Invoice inv : ctx.getInvoices()) {
                if (!userId.equals(inv.getUserId())) continue;
                if (inv.getRemainingBalance() == null
                        || inv.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) continue;
                if (inv.getCreatedAt() == null) continue;

                BigDecimal amtDiff = inv.getRemainingBalance().subtract(p.getAmountPaid()).abs();
                if (amtDiff.compareTo(ctx.getAmountTolerance()) > 0) continue;

                long daysApart = Math.abs(ChronoUnit.DAYS.between(
                        inv.getCreatedAt().toLocalDate(), p.getPaymentDate()));

                if (daysApart <= ctx.getDateToleranceDays()) {
                    ctx.log("  ✓ TXN-" + p.getTransactionId()
                            + " ↔ " + inv.getInvoiceNumber()
                            + " — within " + daysApart + " days of invoice");
                    candidate = inv;
                    break;
                } else {
                    ctx.log("  ⏰ TXN-" + p.getTransactionId()
                            + " ↔ " + inv.getInvoiceNumber()
                            + " — " + daysApart + " days apart (tolerance "
                            + ctx.getDateToleranceDays() + ") REJECTED on date");
                }
            }

            if (candidate != null) {
                ReconciliationMatch m = new ReconciliationMatch();
                m.setInvoiceId(candidate.getInvoiceId());
                m.setPaymentId(p.getPaymentId());
                m.setMatchType(MatchType.TOLERANT);
                m.setConfidenceScore(0.85);
                m.setExplanation("Tolerant: amount + date both within configured windows");
                m.setReversed(false);
                m.setMatchedAt(LocalDateTime.now());
                m.setAmountDifference(BigDecimal.ZERO);
                results.add(m);
            }
        }
        return results;
    }
}
