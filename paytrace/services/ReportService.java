package com.paytrace.services;

import com.paytrace.dao.*;
import com.paytrace.models.AuditEntry;
import com.paytrace.models.Invoice;
import com.paytrace.models.Payment;
import com.paytrace.models.User;
import com.paytrace.models.UserConnection;
import com.paytrace.models.Vendor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link ReportService} (UC-11).
 * Thin facade over the DAO interfaces — keeps the report controller
 * decoupled from JDBC details.
 */
public class ReportService {

    private final InvoiceDAO        invoiceDAO = new InvoiceDAO();
    private final PaymentDAO        paymentDAO = new PaymentDAO();
    private final VendorDAO         vendorDAO  = new VendorDAO();
    private final UserDAO           userDAO    = new UserDAO();
    private final UserConnectionDAO connDAO    = new UserConnectionDAO();
    private final AuditEntryDAO     auditDAO   = new AuditEntryDAO();

    public List<Vendor>  allVendors()  throws Exception { return vendorDAO.findAll();  }
    public List<User>    allUsers()    throws Exception { return userDAO.findAll();    }
    public List<Invoice> allInvoices() throws Exception { return invoiceDAO.findAll(); }
    public List<Payment> allPayments() throws Exception { return paymentDAO.findAll(); }

    public Optional<Vendor> vendor(String vendorId) throws Exception {
        return vendorDAO.findById(vendorId);
    }
    public List<Invoice> vendorInvoices(String vendorId) throws Exception {
        return invoiceDAO.findByOwner(vendorId);
    }
    public List<Payment> vendorPayments(String vendorId) throws Exception {
        return paymentDAO.findByOwner(vendorId);
    }
    public List<UserConnection> vendorConnections(String vendorId) throws Exception {
        return connDAO.findByTarget(vendorId);
    }
    public List<Invoice> vendorInvoicesForUser(String vendorId,
                                               String userId) throws Exception {
        return invoiceDAO.findByOwnerAndUser(vendorId, userId);
    }
    public Optional<User> userById(String userId) throws Exception {
        return userDAO.findById(userId);
    }
    public List<UserConnection> userConnections(String userId) throws Exception {
        return connDAO.findByUser(userId);
    }
    public List<AuditEntry> auditTrail(LocalDate from, LocalDate to) throws Exception {
        if (from != null && to != null) {
            return auditDAO.findByDateRange(
                    LocalDateTime.of(from, java.time.LocalTime.MIN),
                    LocalDateTime.of(to,   java.time.LocalTime.of(23, 59, 59)));
        }
        return auditDAO.findAll();
    }
}
