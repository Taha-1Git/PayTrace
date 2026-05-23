package com.paytrace.patterns.command;

import com.paytrace.dao.InvoiceDAO;
import com.paytrace.dao.PaymentDAO;
import com.paytrace.dao.ReconciliationMatchDAO;
import com.paytrace.models.ReconciliationMatch;
import com.paytrace.models.enums.PaymentStatus;

import java.math.BigDecimal;

/**
 * Command pattern — wraps a reconciliation match so it can be undone/redone.
 * Stores the previous state (invoice balance, payment status) so undo can restore it.
 */
public class MatchCommand implements Command {

    public static final InvoiceDAO             invoiceDAO = new InvoiceDAO();
    public static final PaymentDAO             paymentDAO = new PaymentDAO();
    private static final ReconciliationMatchDAO matchDAO   = new ReconciliationMatchDAO();

    private final ReconciliationMatch match;
    private final String     invoiceId;
    private final String     paymentId;
    private final BigDecimal previousBalance;
    private final BigDecimal newBalance;
    private boolean executed = false;

    public MatchCommand(ReconciliationMatch match,
                        String invoiceId, String paymentId,
                        BigDecimal previousBalance, BigDecimal newBalance) {
        this.match           = match;
        this.invoiceId       = invoiceId;
        this.paymentId       = paymentId;
        this.previousBalance = previousBalance;
        this.newBalance      = newBalance;
    }

    @Override
    public void execute() throws Exception {
        matchDAO.save(match);
        invoiceDAO.updateRemainingBalance(invoiceId, newBalance);
        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            paymentDAO.updateStatus(paymentId, PaymentStatus.FULLY_ALLOCATED);
        }
        executed = true;
    }

    @Override
    public void undo() throws Exception {
        if (!executed) return;
        invoiceDAO.updateRemainingBalance(invoiceId, previousBalance);
        paymentDAO.updateStatus(paymentId, PaymentStatus.UNMATCHED);
        // Note: we keep the ReconciliationMatch row but mark it reversed
        matchDAO.markReversed(match.getMatchId());
    }

    @Override
    public String getDescription() {
        return "Match " + invoiceId.substring(0, 8) + "... ↔ "
                + paymentId.substring(0, 8) + "...";
    }
}