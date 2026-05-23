package com.paytrace.services;

import com.paytrace.dao.*;
import com.paytrace.models.*;
import com.paytrace.models.enums.InvoiceStatus;
import com.paytrace.models.enums.PaymentStatus;
import com.paytrace.patterns.command.CommandHistory;
import com.paytrace.patterns.command.MatchCommand;
import com.paytrace.patterns.factory.StrategyFactory;
import com.paytrace.patterns.strategy.MatchingStrategy;
import com.paytrace.patterns.strategy.ReconciliationContext;
import com.paytrace.services.NotificationService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ReconciliationService {

    public static final class RunSummary {
        public final int totalInvoicesConsidered;
        public final int totalPaymentsConsidered;
        public final int matchedCount;
        public final int refundedOverpaymentCount;
        public final BigDecimal totalRefunded;

        public RunSummary(int totalInvoicesConsidered,
                          int totalPaymentsConsidered,
                          int matchedCount,
                          int refundedOverpaymentCount,
                          BigDecimal totalRefunded) {
            this.totalInvoicesConsidered  = totalInvoicesConsidered;
            this.totalPaymentsConsidered  = totalPaymentsConsidered;
            this.matchedCount             = matchedCount;
            this.refundedOverpaymentCount = refundedOverpaymentCount;
            this.totalRefunded            = totalRefunded == null
                    ? BigDecimal.ZERO : totalRefunded;
        }
    }

    private final InvoiceDAO             invoiceDAO = new InvoiceDAO();
    private final PaymentDAO             paymentDAO = new PaymentDAO();
    private final ReconciliationMatchDAO matchDAO   = new ReconciliationMatchDAO();
    private final UserConnectionDAO      connDAO    = new UserConnectionDAO();
    private final UserDAO                userDAO    = new UserDAO();
    private final AuditEntryDAO          auditDAO   = new AuditEntryDAO();

    private final CommandHistory commandHistory = CommandHistory.getInstance();

    public RunSummary runAutoReconciliation(String vendorId,
                                            BigDecimal amountTolerance,
                                            int dateToleranceDays,
                                            Consumer<String> log) throws Exception {
        if (vendorId == null || vendorId.isBlank())
            throw new IllegalArgumentException("Vendor context required for auto reconciliation.");
        if (amountTolerance == null || amountTolerance.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Amount tolerance must be a non-negative number.");
        if (dateToleranceDays < 0)
            throw new IllegalArgumentException("Date tolerance must be a non-negative number of days.");

        Consumer<String> safeLog = log != null ? log : msg -> {};

        List<Invoice>        invoices = new ArrayList<>(invoiceDAO.findByOwner(vendorId));
        List<Payment>        payments = new ArrayList<>(paymentDAO.findByOwner(vendorId));
        List<UserConnection> conns    = connDAO.findByTarget(vendorId);

        safeLog.accept("Loaded " + invoices.size() + " invoice(s), "
                + payments.size() + " payment(s), "
                + conns.size() + " user connection(s).\n");

        // ── Pre-pass: overpayment detection + refund ─────────────────
        int refundedCount = 0;
        BigDecimal totalRefunded = BigDecimal.ZERO;

        for (Payment p : new ArrayList<>(payments)) {
            if (p.getStatus() == PaymentStatus.FULLY_ALLOCATED) continue;
            if (p.getSenderAccount() == null) continue;

            String userId = findUserIdForAccount(conns, p.getSenderAccount());
            if (userId == null) continue;

            for (Invoice inv : invoices) {
                if (!userId.equals(inv.getUserId())) continue;
                if (inv.getRemainingBalance() == null) continue;
                if (inv.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) continue;

                if (p.getAmountPaid().compareTo(inv.getRemainingBalance()) > 0) {
                    BigDecimal overpay = p.getAmountPaid().subtract(inv.getRemainingBalance());

                    safeLog.accept("  ⚠ Overpayment on TXN-" + p.getTransactionId()
                            + ": paid " + p.getAmountPaid()
                            + ", invoice owed " + inv.getRemainingBalance()
                            + " — refunding " + overpay
                            + " to " + p.getSenderAccount() + "\n");

                    issueRefundEmail(userId, p, inv, overpay);
                    paymentDAO.markRefunded(p.getPaymentId(), overpay);
                    p.setAmountPaid(inv.getRemainingBalance());
                    totalRefunded = totalRefunded.add(overpay);
                    refundedCount++;
                    break;
                }
            }
        }
        if (refundedCount > 0) safeLog.accept("\n");

        // ── Strategy execution ───────────────────────────────────────
        safeLog.accept("Running strategies in order:\n\n");
        ReconciliationContext ctx = new ReconciliationContext(
                invoices, payments, conns,
                amountTolerance, dateToleranceDays,
                msg -> safeLog.accept(msg + "\n"));

        List<ReconciliationMatch> allMatches = new ArrayList<>();
        for (MatchingStrategy strategy : StrategyFactory.getInstance().createAll()) {
            List<ReconciliationMatch> matches = strategy.execute(ctx);
            allMatches.addAll(matches);
            for (ReconciliationMatch m : matches) {
                applyMatch(m, invoices, payments);
            }
            safeLog.accept("");
        }

        // ── Post-run: overdue unmatched warnings ─────────────────────
        int overdueWarned = notifyOverdueUnmatched(invoices, dateToleranceDays);
        if (overdueWarned > 0) {
            safeLog.accept("\nSent " + overdueWarned
                    + " overdue-warning email(s) to users with unmatched invoices.\n");
        }

        // ── Audit ────────────────────────────────────────────────────
        try {
            AuditEntry e = new AuditEntry("RECONCILIATION_RUN",
                    null, "Vendor", vendorId, null, null);
            auditDAO.save(e);
        } catch (Exception e) {
            System.err.println("Audit save failed: " + e.getMessage());
            e.printStackTrace();
        }

        return new RunSummary(
                invoices.size() + allMatches.size(),
                payments.size() + allMatches.size(),
                allMatches.size(),
                refundedCount,
                totalRefunded);
    }

    public void persistManualMatch(ReconciliationMatch match,
                                   String invoiceId, String paymentId,
                                   BigDecimal previousBalance,
                                   BigDecimal newBalance,
                                   String performedBy) throws Exception {
        MatchCommand cmd = new MatchCommand(match, invoiceId, paymentId,
                previousBalance, newBalance);
        commandHistory.executeAndStore(cmd);


        try {
            AuditEntry e = new AuditEntry("MANUAL_MATCH", performedBy,
                    "Invoice", invoiceId, null, null);
            auditDAO.save(e);
        } catch (Exception e) {
            System.err.println("Audit save failed (manual match): " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void reverseMatch(String matchId) throws Exception {
        matchDAO.markReversed(matchId);
    }

    public List<ReconciliationMatch> findByInvoiceId(String invoiceId) throws Exception {
        return matchDAO.findByInvoiceId(invoiceId);
    }

    /* ─── Internal helpers ──────────────────────────────────────────── */

    private void applyMatch(ReconciliationMatch m,
                            List<Invoice> invoicePool,
                            List<Payment> paymentPool) throws Exception {
        Invoice invoice = findIn(invoicePool, i -> i.getInvoiceId().equals(m.getInvoiceId()));
        Payment payment = findIn(paymentPool, p -> p.getPaymentId().equals(m.getPaymentId()));
        if (invoice == null || payment == null) return;

        BigDecimal previousBalance = invoice.getRemainingBalance();
        BigDecimal newBalance = previousBalance.subtract(payment.getAmountPaid());
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) newBalance = BigDecimal.ZERO;

        MatchCommand cmd = new MatchCommand(m, invoice.getInvoiceId(),
                payment.getPaymentId(), previousBalance, newBalance);
        commandHistory.executeAndStore(cmd);

        // Persist the new statuses so dashboard / reports see them
        try {
            if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
                invoiceDAO.updateAfterMatch(invoice.getInvoiceId(),
                        InvoiceStatus.MATCHED,
                        BigDecimal.ZERO);
                paymentDAO.updateAfterMatch(payment.getPaymentId(),
                        PaymentStatus.FULLY_ALLOCATED);
            } else {
                invoiceDAO.updateAfterMatch(invoice.getInvoiceId(),
                        InvoiceStatus.PARTIALLY_MATCHED,
                        newBalance);
                paymentDAO.updateAfterMatch(payment.getPaymentId(),
                        PaymentStatus.PARTIALLY_ALLOCATED);
            }
        } catch (Exception e) { e.printStackTrace(); }

        notifyUserOfMatch(invoice, payment, previousBalance);
    }

    private static <T> T findIn(List<T> list, java.util.function.Predicate<T> pred) {
        for (T t : list) if (pred.test(t)) return t;
        return null;
    }

    private String findUserIdForAccount(List<UserConnection> conns, String account) {
        for (UserConnection uc : conns)
            if (account.equals(uc.getUserAccountNumber())) return uc.getUserId();
        return null;
    }

    private void issueRefundEmail(String userId, Payment p, Invoice inv, BigDecimal overpay) {
        try {
            String body = "Hello,\n\n"
                    + "You paid PKR " + p.getAmountPaid()
                    + " for invoice " + inv.getInvoiceNumber()
                    + " which only required PKR " + inv.getRemainingBalance() + ".\n\n"
                    + "The excess amount of PKR " + overpay
                    + " has been transferred back to your account "
                    + p.getSenderAccount() + ".\n\n"
                    + "Your invoice is now fully paid. Thank you for your business.";

            NotificationService.notify(
                    userId,
                    Notification.Type.REFUND_ISSUED,
                    "Overpayment refunded — PKR " + overpay,
                    body,
                    "Invoice",
                    inv.getInvoiceId(),
                    true);  // alsoEmail = true ✅

        } catch (Exception e) {
            System.err.println("issueRefundEmail failed for userId=" + userId);
            e.printStackTrace();
        }
    }

    /**
     * @param previousBalance  the invoice's remaining balance BEFORE this payment
     *                         was applied — used to correctly determine full vs partial.
     */
    private void notifyUserOfMatch(Invoice inv, Payment p, BigDecimal previousBalance) {
        if (inv.getUserId() == null) return;
        try {
            Optional<User> u = userDAO.findById(inv.getUserId());
            if (u.isEmpty()) {
                System.err.println("notifyUserOfMatch: no user found for id=" + inv.getUserId());
                return;
            }

            // ✅ Use previousBalance (before deduction) to decide full vs partial
            BigDecimal newBalance = previousBalance.subtract(p.getAmountPaid());
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) newBalance = BigDecimal.ZERO;
            boolean fullyPaid = newBalance.compareTo(BigDecimal.ZERO) == 0;

            String title;
            String body;
            Notification.Type type;

            if (fullyPaid) {
                type  = Notification.Type.PAYMENT_COMPLETED;
                title = "Invoice fully paid — " + inv.getInvoiceNumber();
                body  = "Hello " + u.get().getFullName() + ",\n\n"
                        + "Your payment of PKR " + p.getAmountPaid()
                        + " for invoice " + inv.getInvoiceNumber()
                        + " has been received and reconciled.\n\n"
                        + "This invoice is now fully paid. Thank you for your business.";
            } else {
                type  = Notification.Type.PAYMENT_RECEIVED;
                title = "Partial payment received — " + inv.getInvoiceNumber();
                body  = "Hello " + u.get().getFullName() + ",\n\n"
                        + "Your partial payment of PKR " + p.getAmountPaid()
                        + " for invoice " + inv.getInvoiceNumber()
                        + " has been received.\n\n"
                        + "Remaining outstanding balance: PKR " + newBalance
                        + ".\n\nPlease clear the remaining balance by the due date.";
            }

            System.out.println(">>> Sending email to userId=" + inv.getUserId()
                    + " fullyPaid=" + fullyPaid
                    + " email=" + u.get().getEmail());  // ✅ debug line

            NotificationService.notify(
                    inv.getUserId(),
                    type,
                    title,
                    body,
                    "Invoice",
                    inv.getInvoiceId(),
                    true);  // alsoEmail = true ✅

        } catch (Exception e) {
            System.err.println("notifyUserOfMatch failed for invoice=" + inv.getInvoiceNumber());
            e.printStackTrace();
        }
    }

    private int notifyOverdueUnmatched(List<Invoice> stillUnmatched,
                                       int dateToleranceDays) {
        int count = 0;
        java.time.LocalDate today = java.time.LocalDate.now();

        for (Invoice inv : stillUnmatched) {
            if (inv.getDueDate() == null) continue;
            if (inv.getRemainingBalance() == null
                    || inv.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) continue;

            long daysOverdue = java.time.temporal.ChronoUnit.DAYS
                    .between(inv.getDueDate(), today);
            if (daysOverdue <= dateToleranceDays) continue;
            if (inv.getUserId() == null) continue;

            try {
                Optional<User> u = userDAO.findById(inv.getUserId());
                if (u.isEmpty()) continue;

                String body = "Hello " + u.get().getFullName() + ",\n\n"
                        + "Invoice " + inv.getInvoiceNumber()
                        + " (PKR " + inv.getAmountDue()
                        + ") was due on " + inv.getDueDate()
                        + " and is now " + daysOverdue + " day(s) overdue.\n\n"
                        + "No matching payment was found during the latest "
                        + "auto-reconciliation run. Please pay the outstanding "
                        + "balance of PKR " + inv.getRemainingBalance()
                        + " as soon as possible to avoid further notices.";

                NotificationService.notify(
                        inv.getUserId(),
                        Notification.Type.OVERDUE_WARNING,
                        "Overdue invoice — " + inv.getInvoiceNumber(),
                        body,
                        "Invoice",
                        inv.getInvoiceId(),
                        true);
                count++;

            } catch (Exception e) {
                System.err.println("notifyOverdueUnmatched failed for invoice="
                        + inv.getInvoiceNumber());
                e.printStackTrace();
            }
        }
        return count;
    }
}