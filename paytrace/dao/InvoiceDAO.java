package com.paytrace.dao;

import com.paytrace.config.DatabaseConnection;
import com.paytrace.models.Invoice;
import com.paytrace.models.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InvoiceDAO {

    private Connection getConn() throws SQLException {
        try { return DatabaseConnection.getInstance().getConnection(); }
        catch (Exception e) { throw new SQLException(e.getMessage(), e); }
    }
    private void releaseConn(Connection c) {
        if (c != null) try { DatabaseConnection.getInstance().releaseConnection(c); }
        catch (Exception ignored) {}
    }
    public void save(Invoice inv) throws SQLException {
        String sql = "INSERT INTO invoices " +
                "(invoice_id, invoice_number, owner_type, owner_id, user_id, " +
                "counterparty, description, amount_due, remaining_balance, status, due_date) " +
                "VALUES (NEWID(),?,?,?,?,?,?,?,?,?,?)";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, inv.getInvoiceNumber());
            ps.setString(2, inv.getOwnerType() != null ? inv.getOwnerType() : "VENDOR");
            ps.setString(3, inv.getOwnerId());
            ps.setString(4, inv.getUserId());
            ps.setString(5, inv.getCounterparty());
            ps.setString(6, inv.getDescription());
            ps.setBigDecimal(7, inv.getAmountDue());
            ps.setBigDecimal(8, inv.getAmountDue());
            ps.setString(9, inv.getStatus() != null ? inv.getStatus().name()
                    : InvoiceStatus.PENDING_APPROVAL.name());
            ps.setDate(10, inv.getDueDate() != null ? Date.valueOf(inv.getDueDate()) : null);
            ps.executeUpdate();
        } finally { releaseConn(c); }
    }
    public Optional<Invoice> findById(String invoiceId) throws SQLException {
        return queryOne("SELECT * FROM invoices WHERE invoice_id = ?", invoiceId);
    }
    public List<Invoice> findByStatus(InvoiceStatus status) throws SQLException {
        return query("SELECT * FROM invoices WHERE status = ? ORDER BY created_at DESC",
                status.name());
    }
    public List<Invoice> findByOwner(String ownerId) throws SQLException {
        return query("SELECT * FROM invoices WHERE owner_id = ? ORDER BY due_date",
                ownerId);
    }
    public List<Invoice> findByUser(String userId) throws SQLException {
        return query("SELECT * FROM invoices WHERE user_id = ? ORDER BY due_date",
                userId);
    }
    public List<Invoice> findByOwnerAndUser(String ownerId, String userId)
            throws SQLException {
        String sql = "SELECT * FROM invoices WHERE owner_id = ? AND user_id = ? " +
                "ORDER BY due_date";
        List<Invoice> list = new ArrayList<>();
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ownerId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } finally { releaseConn(c); }
        return list;
    }
    public void updateStatus(String invoiceId, InvoiceStatus status,
                             String approvedBy, String holdReason) throws SQLException {
        String sql = "UPDATE invoices SET status=?, approved_by=?, hold_reason=? " +
                "WHERE invoice_id=?";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, approvedBy);
            ps.setString(3, holdReason);
            ps.setString(4, invoiceId);
            ps.executeUpdate();
        } finally { releaseConn(c); }
    }
    public void updateRemainingBalance(String invoiceId, BigDecimal balance)
            throws SQLException {
        String sql = "UPDATE invoices SET remaining_balance=? WHERE invoice_id=?";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, balance);
            ps.setString(2, invoiceId);
            ps.executeUpdate();
        } finally { releaseConn(c); }
    }
    public List<Invoice> findOverdue() throws SQLException {
        String sql = "SELECT * FROM invoices WHERE due_date < GETDATE() " +
                "AND status NOT IN ('REJECTED','APPROVED') ORDER BY due_date";
        List<Invoice> list = new ArrayList<>();
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } finally { releaseConn(c); }
        return list;
    }
    public List<Invoice> findAll() throws SQLException {
        String sql = "SELECT * FROM invoices ORDER BY created_at DESC";
        List<Invoice> list = new ArrayList<>();
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } finally { releaseConn(c); }
        return list;
    }

    // ── Helpers ──
    private List<Invoice> query(String sql, String param) throws SQLException {
        List<Invoice> list = new ArrayList<>();
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } finally { releaseConn(c); }
        return list;
    }
    private Optional<Invoice> queryOne(String sql, String param) throws SQLException {
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } finally { releaseConn(c); }
        return Optional.empty();
    }

    /** Reconciliation-side update — sets new status and new remaining balance,
     *  leaves approved_by / hold_reason untouched. */
    public void updateAfterMatch(String invoiceId,
                                 InvoiceStatus status,
                                 BigDecimal newRemainingBalance) throws SQLException {
        String sql = "UPDATE invoices SET status = ?, remaining_balance = ? " +
                "WHERE invoice_id = ?";
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setBigDecimal(2, newRemainingBalance);
            ps.setString(3, invoiceId);
            ps.executeUpdate();
        } finally { releaseConn(conn); }
    }

    private Invoice mapRow(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice();
        inv.setInvoiceId(rs.getString("invoice_id"));
        inv.setInvoiceNumber(rs.getString("invoice_number"));
        inv.setOwnerType(rs.getString("owner_type"));
        inv.setOwnerId(rs.getString("owner_id"));
        inv.setUserId(rs.getString("user_id"));
        inv.setCounterparty(rs.getString("counterparty"));
        inv.setDescription(rs.getString("description"));
        inv.setAmountDue(rs.getBigDecimal("amount_due"));
        inv.setRemainingBalance(rs.getBigDecimal("remaining_balance"));
        String st = rs.getString("status");
        if (st != null) inv.setStatus(InvoiceStatus.valueOf(st));
        Date dd = rs.getDate("due_date");
        if (dd != null) inv.setDueDate(dd.toLocalDate());
        inv.setApprovedBy(rs.getString("approved_by"));
        inv.setHoldReason(rs.getString("hold_reason"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) inv.setCreatedAt(ts.toLocalDateTime());
        return inv;
    }
    public int countUnreconciled(String vendorId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM invoices " +
                "WHERE owner_id = ? AND remaining_balance > 0";
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vendorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } finally { releaseConn(conn); }
        return 0;
    }
}