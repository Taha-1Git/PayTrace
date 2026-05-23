package com.paytrace.services;

import com.paytrace.dao.PaymentAllocationDAO;
import com.paytrace.dao.PaymentDAO;
import com.paytrace.dao.AuditEntryDAO;
import com.paytrace.dao.PaymentAllocationDAO;
import com.paytrace.dao.PaymentDAO;
import com.paytrace.models.AuditEntry;
import com.paytrace.models.Payment;
import com.paytrace.models.PaymentAllocation;
import com.paytrace.models.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link PaymentService}.
 * Encapsulates transaction-id generation and amount validation so callers
 * don't have to duplicate those rules in every controller.
 */
public class PaymentService {

    private final PaymentDAO           paymentDAO = new PaymentDAO();
    private final PaymentAllocationDAO allocDAO   = new PaymentAllocationDAO();
    private final AuditEntryDAO    auditDAO   = new AuditEntryDAO();
    public Payment recordPayment(String ownerId,
                                 String counterpartyName,
                                 String senderAccount,
                                 BigDecimal amount,
                                 PaymentStatus initialStatus) throws Exception {

        if (ownerId == null || ownerId.isBlank())
            throw new IllegalArgumentException("Owner (vendor) ID is required.");
        if (counterpartyName == null || counterpartyName.isBlank())
            throw new IllegalArgumentException("Counterparty name is required.");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be positive.");

        Payment p = new Payment();
        p.setTransactionId("PTR-" + System.currentTimeMillis());
        p.setOwnerType("VENDOR");
        p.setOwnerId(ownerId);
        p.setCounterparty(counterpartyName);
        p.setNormalizedParty(counterpartyName.toLowerCase());
        p.setSenderAccount(senderAccount);
        p.setAmountPaid(amount);
        p.setUnallocatedAmount(initialStatus == PaymentStatus.FULLY_ALLOCATED
                ? BigDecimal.ZERO : amount);
        p.setPaymentDate(LocalDate.now());
        p.setStatus(initialStatus == null ? PaymentStatus.UNMATCHED : initialStatus);
        paymentDAO.save(p);
        return p;
    }
    public Payment recordFullyAllocatedPayment(String ownerId,
                                               String counterpartyName,
                                               String senderAccount,
                                               BigDecimal amount) throws Exception {
        return recordPayment(ownerId, counterpartyName, senderAccount,
                amount, PaymentStatus.FULLY_ALLOCATED);
    }
    public void updateStatus(String paymentId, PaymentStatus status) throws Exception {
        paymentDAO.updateStatus(paymentId, status);
    }
    public void splitPayment(String paymentId,
                             List<PaymentAllocation> allocations,
                             String performedBy) throws Exception {
        if (paymentId == null || paymentId.isBlank())
            throw new IllegalArgumentException("paymentId is required.");
        if (allocations == null || allocations.isEmpty())
            throw new IllegalArgumentException("At least one allocation is required.");

        for (PaymentAllocation a : allocations) {
            if (a == null) continue;
            // Make sure each allocation references this payment.
            a.setPaymentId(paymentId);
            allocDAO.save(a);
        }
        paymentDAO.updateStatus(paymentId, PaymentStatus.FULLY_ALLOCATED);
        try {
            AuditEntry e = new AuditEntry("PAYMENT_SPLIT", performedBy,
                    "Payment", paymentId, null, null);
            auditDAO.save(e);
        } catch (Exception ignored) {}
    }

    /* ─── Read-through queries ─────────────────────────────────────── */

     public Optional<Payment> findById(String paymentId) throws Exception {
        return paymentDAO.findById(paymentId);
    }
     public List<Payment> findByStatus(PaymentStatus status) throws Exception {
        return paymentDAO.findByStatus(status);
    }
    public List<Payment> findByOwner(String ownerId) throws Exception {
        return paymentDAO.findByOwner(ownerId);
    }
     public List<Payment> findBySenderAccount(String senderAccount) throws Exception {
        return paymentDAO.findBySenderAccount(senderAccount);
    }
     public List<Payment> findAll() throws Exception {
        return paymentDAO.findAll();
    }
}
