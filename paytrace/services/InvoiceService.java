package com.paytrace.services;

import com.paytrace.dao.InvoiceDAO;
import com.paytrace.dao.InvoiceDAO;
import com.paytrace.models.Alert;
import com.paytrace.models.AuditEntry;
import com.paytrace.models.Invoice;
import com.paytrace.models.enums.AlertStatus;
import com.paytrace.models.enums.AlertType;
import com.paytrace.models.enums.InvoiceStatus;
import com.paytrace.models.enums.Severity;
import com.paytrace.services.AlertService;
import com.paytrace.dao.AuditEntryDAO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link InvoiceService}.
 *
 * Demonstrates clean GRASP layering:
 *  - {@code Information Expert} — DAOs own SQL; this service owns business rules.
 *  - {@code Low Coupling}       — depends on the {@link InvoiceDAO} interface only.
 *  - {@code Controller}         — invoice-related side-effects (alerts, audit log)
 *                                 happen here so controllers stay thin.
 */
public class InvoiceService {

    private final InvoiceDAO        invoiceDAO   = new InvoiceDAO();
    private final AuditEntryDAO auditDAO     = new AuditEntryDAO();
    private final AlertService      alertService = AlertService.getInstance();

    /* ─── Lifecycle ─────────────────────────────────────────────────── */
    public Invoice createInvoice(Invoice draft) throws Exception {
        validate(draft);
        if (draft.getStatus() == null) draft.setStatus(InvoiceStatus.PENDING_APPROVAL);
        if (draft.getRemainingBalance() == null)
            draft.setRemainingBalance(draft.getAmountDue());
        invoiceDAO.save(draft);
        audit("INVOICE_CREATED", draft.getInvoiceId(), draft.getCounterparty());
        return draft;
    }
    public void approve(String invoiceId, String approvedByName) throws Exception {
        invoiceDAO.updateStatus(invoiceId, InvoiceStatus.APPROVED, approvedByName, null);
        audit("INVOICE_APPROVED", invoiceId, approvedByName);
    }
    public void placeOnHold(String invoiceId, String reason, String adminName) throws Exception {
        invoiceDAO.updateStatus(invoiceId, InvoiceStatus.ON_HOLD, adminName, reason);
        audit("INVOICE_ON_HOLD", invoiceId, adminName);

        // Raise a Warning alert through the Subject so observers (Dashboard) react.
        Optional<Invoice> inv = invoiceDAO.findById(invoiceId);
        if (inv.isPresent()) {
            Alert a = new Alert();
            a.setAlertType(AlertType.LARGE_DISCREPANCY);
            a.setSeverity(Severity.WARNING);
            a.setDescription("Invoice " + inv.get().getInvoiceNumber()
                    + " placed on HOLD. Reason: " + reason);
            a.setStatus(AlertStatus.ACTIVE);
            a.setEntityId(invoiceId);
            a.setEntityType("Invoice");
            int age = inv.get().getCreatedAt() == null ? 0
                    : (int) ChronoUnit.DAYS.between(
                    inv.get().getCreatedAt().toLocalDate(), LocalDate.now());
            a.setAgeInDays(age);
            alertService.raiseAlert(a);
        }
    }
    public void reject(String invoiceId, String reason, String adminName) throws Exception {
        invoiceDAO.updateStatus(invoiceId, InvoiceStatus.REJECTED, adminName, reason);
        audit("INVOICE_REJECTED", invoiceId, adminName);
    }
    public void updateRemainingBalance(String invoiceId, BigDecimal newBalance)
            throws Exception {
        if (newBalance == null) throw new IllegalArgumentException("newBalance is null");
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) newBalance = BigDecimal.ZERO;
        invoiceDAO.updateRemainingBalance(invoiceId, newBalance);
    }

    /* ─── Reads — thin facade over DAO ──────────────────────────────── */

     public Optional<Invoice> findById(String invoiceId) throws Exception {
        return invoiceDAO.findById(invoiceId);
    }
     public List<Invoice> findByStatus(InvoiceStatus status) throws Exception {
        return invoiceDAO.findByStatus(status);
    }
     public List<Invoice> findByOwner(String ownerId) throws Exception {
        return invoiceDAO.findByOwner(ownerId);
    }
     public List<Invoice> findByUser(String userId) throws Exception {
        return invoiceDAO.findByUser(userId);
    }
     public List<Invoice> findByOwnerAndUser(String ownerId, String userId)
            throws Exception {
        return invoiceDAO.findByOwnerAndUser(ownerId, userId);
    }
    public List<Invoice> findOverdue() throws Exception {
        return invoiceDAO.findOverdue();
    }
     public List<Invoice> findAll() throws Exception {
        return invoiceDAO.findAll();
    }

    /* ─── Internal helpers ──────────────────────────────────────────── */

    private void validate(Invoice inv) {
        if (inv == null) throw new IllegalArgumentException("Invoice is null.");
        if (inv.getInvoiceNumber() == null || inv.getInvoiceNumber().isBlank())
            throw new IllegalArgumentException("Invoice number is required.");
        if (inv.getOwnerId() == null)
            throw new IllegalArgumentException("Owner (vendor) is required.");
        if (inv.getAmountDue() == null
                || inv.getAmountDue().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be positive.");
        if (inv.getDueDate() == null)
            throw new IllegalArgumentException("Due date is required.");
    }

    private void audit(String event, String entityId, String performedBy) {
        try {
            AuditEntry e = new AuditEntry(event, performedBy,
                    "Invoice", entityId, null, null);
            auditDAO.save(e);
        } catch (Exception ex) {
            System.err.println("Audit log failed for " + event + ": " + ex.getMessage());
        }
    }
}
