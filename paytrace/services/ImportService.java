package com.paytrace.services;

import com.paytrace.dao.ImportSessionDAO;
import com.paytrace.dao.InvoiceDAO;
import com.paytrace.dao.PaymentDAO;
import com.paytrace.dao.ImportSessionDAO;
import com.paytrace.dao.InvoiceDAO;
import com.paytrace.dao.PaymentDAO;
import com.paytrace.models.ImportSession;
import com.paytrace.models.Invoice;
import com.paytrace.models.Payment;
import com.paytrace.models.enums.ImportStatus;
import com.paytrace.models.enums.ImportType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Default implementation of {@link ImportService} (UC-02, UC-03).
 *
 * Validates each draft, persists the valid ones, and writes a single
 * {@link ImportSession} record summarising the run. Controllers no longer
 * need to know about {@code PaymentDAO} or {@code ImportSessionDAO}.
 */
public class ImportService {

    public static final class ImportResult {
            public final int successCount;
            public final int failureCount;
            public final String importSessionId;
    
            public ImportResult(int successCount, int failureCount, String importSessionId) {
                this.successCount    = successCount;
                this.failureCount    = failureCount;
                this.importSessionId = importSessionId;
            }
        }


    private final PaymentDAO       paymentDAO = new PaymentDAO();
    private final InvoiceDAO       invoiceDAO = new InvoiceDAO();
    private final ImportSessionDAO importDAO  = new ImportSessionDAO();
    public ImportResult importBankStatement(String vendorId,
                                            List<Payment> payments,
                                            String sourceFileName,
                                            String performedByName) throws Exception {
        if (vendorId == null || vendorId.isBlank()) {
            throw new IllegalArgumentException("Vendor ID required for import.");
        }
        if (payments == null) payments = java.util.Collections.emptyList();

        int success = 0, fail = 0;
        for (Payment p : payments) {
            if (!isValidPayment(p)) { fail++; continue; }
            try {
                p.setOwnerType("VENDOR");
                p.setOwnerId(vendorId);
                paymentDAO.save(p);
                success++;
            } catch (Exception e) {
                fail++;
            }
        }
        String sessionId = recordSession(ImportType.BANK_STATEMENT,
                sourceFileName, performedByName, success, fail);
        return new ImportResult(success, fail, sessionId);
    }
    public ImportResult importInvoices(String vendorId,
                                       List<Invoice> invoices,
                                       String sourceFileName,
                                       String performedByName) throws Exception {
        if (vendorId == null || vendorId.isBlank()) {
            throw new IllegalArgumentException("Vendor ID required for import.");
        }
        if (invoices == null) invoices = java.util.Collections.emptyList();

        int success = 0, fail = 0;
        for (Invoice inv : invoices) {
            if (!isValidInvoice(inv)) { fail++; continue; }
            try {
                inv.setOwnerType("VENDOR");
                inv.setOwnerId(vendorId);
                if (inv.getRemainingBalance() == null)
                    inv.setRemainingBalance(inv.getAmountDue());
                invoiceDAO.save(inv);
                success++;
            } catch (Exception e) {
                fail++;
            }
        }
        String sessionId = recordSession(ImportType.INVOICE_FILE,
                sourceFileName, performedByName, success, fail);
        return new ImportResult(success, fail, sessionId);
    }

    /* ─── Validation ────────────────────────────────────────────────── */

    private boolean isValidPayment(Payment p) {
        if (p == null) return false;
        if (p.getTransactionId() == null || p.getTransactionId().isBlank()) return false;
        if (p.getAmountPaid() == null
                || p.getAmountPaid().compareTo(BigDecimal.ZERO) <= 0) return false;
        if (p.getPaymentDate() == null) return false;
        if (p.getSenderAccount() == null || p.getSenderAccount().isBlank()) return false;
        return true;
    }

    private boolean isValidInvoice(Invoice inv) {
        if (inv == null) return false;
        if (inv.getInvoiceNumber() == null || inv.getInvoiceNumber().isBlank()) return false;
        if (inv.getAmountDue() == null
                || inv.getAmountDue().compareTo(BigDecimal.ZERO) <= 0) return false;
        if (inv.getDueDate() == null) return false;
        return true;
    }

    /* ─── Session bookkeeping ──────────────────────────────────────── */

    private String recordSession(ImportType type, String fileName,
                                 String performedByName,
                                 int success, int fail) {
        try {
            ImportSession s = new ImportSession();
            s.setImportType(type);
            s.setFileName(fileName);
            s.setImportedBy(performedByName);
            s.setSuccessCount(success);
            s.setFailureCount(fail);
            s.setImportedAt(LocalDateTime.now());
            s.setStatus(fail == 0
                    ? ImportStatus.COMPLETED
                    : (success > 0 ? ImportStatus.PARTIAL_SUCCESS : ImportStatus.FAILED));
            importDAO.save(s);
            return s.getSessionId();
        } catch (Exception ex) {
            System.err.println("Could not save import session: " + ex.getMessage());
            return null;
        }
    }
}
