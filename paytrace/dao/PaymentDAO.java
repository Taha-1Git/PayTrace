package com.paytrace.dao;

import com.paytrace.config.DatabaseConnection;
import com.paytrace.models.Payment;
import com.paytrace.models.enums.PaymentStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PaymentDAO {

    private Connection getConn() throws SQLException {
        try { return DatabaseConnection.getInstance().getConnection(); }
        catch (Exception e) { throw new SQLException(e.getMessage(), e); }
    }
    private void releaseConn(Connection c) {
        if (c != null) try { DatabaseConnection.getInstance().releaseConnection(c); }
        catch (Exception ignored) {}
    }
    public void save(Payment p) throws SQLException {
        String sql = "INSERT INTO payments " +
                "(payment_id, transaction_id, owner_type, owner_id, counterparty, " +
                "normalized_party, sender_account, amount_paid, unallocated_amount, " +
                "payment_date, status, confirmed) " +
                "VALUES (NEWID(),?,?,?,?,?,?,?,?,?,?,0)";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getTransactionId());
            ps.setString(2, p.getOwnerType() != null ? p.getOwnerType() : "VENDOR");
            ps.setString(3, p.getOwnerId());
            ps.setString(4, p.getCounterparty());
            ps.setString(5, p.getNormalizedParty());
            ps.setString(6, p.getSenderAccount());
            ps.setBigDecimal(7, p.getAmountPaid());
            ps.setBigDecimal(8, p.getUnallocatedAmount() != null
                    ? p.getUnallocatedAmount() : p.getAmountPaid());
            ps.setDate(9, p.getPaymentDate() != null ? Date.valueOf(p.getPaymentDate()) : null);
            ps.setString(10, p.getStatus() != null ? p.getStatus().name()
                    : PaymentStatus.UNMATCHED.name());
            ps.executeUpdate();
        } finally { releaseConn(c); }
    }
    public Optional<Payment> findById(String paymentId) throws SQLException {
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM payments WHERE payment_id = ?")) {
            ps.setString(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } finally { releaseConn(c); }
        return Optional.empty();
    }
    public List<Payment> findByStatus(PaymentStatus status) throws SQLException {
        return query("SELECT * FROM payments WHERE status = ? ORDER BY payment_date DESC",
                status.name());
    }
    public List<Payment> findByOwner(String ownerId) throws SQLException {
        return query("SELECT * FROM payments WHERE owner_id = ? ORDER BY payment_date DESC",
                ownerId);
    }
    public List<Payment> findBySenderAccount(String senderAccount) throws SQLException {
        return query("SELECT * FROM payments WHERE sender_account = ? " +
                "ORDER BY payment_date DESC", senderAccount);
    }
    public void updateStatus(String paymentId, PaymentStatus status) throws SQLException {
        String sql = "UPDATE payments SET status=? WHERE payment_id=?";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, paymentId);
            ps.executeUpdate();
        } finally { releaseConn(c); }
    }
    public List<Payment> findAll() throws SQLException {
        String sql = "SELECT * FROM payments ORDER BY payment_date DESC";
        List<Payment> list = new ArrayList<>();
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } finally { releaseConn(c); }
        return list;
    }

    private List<Payment> query(String sql, String param) throws SQLException {
        List<Payment> list = new ArrayList<>();
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } finally { releaseConn(c); }
        return list;
    }

    private Payment mapRow(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getString("payment_id"));
        p.setTransactionId(rs.getString("transaction_id"));
        p.setOwnerType(rs.getString("owner_type"));
        p.setOwnerId(rs.getString("owner_id"));
        p.setCounterparty(rs.getString("counterparty"));
        p.setNormalizedParty(rs.getString("normalized_party"));
        p.setSenderAccount(rs.getString("sender_account"));
        p.setAmountPaid(rs.getBigDecimal("amount_paid"));
        p.setUnallocatedAmount(rs.getBigDecimal("unallocated_amount"));
        Date pd = rs.getDate("payment_date");
        if (pd != null) p.setPaymentDate(pd.toLocalDate());
        String st = rs.getString("status");
        if (st != null) p.setStatus(PaymentStatus.valueOf(st));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) p.setCreatedAt(ts.toLocalDateTime());
        // New v6 fields
        try { p.setConfirmed(rs.getBoolean("confirmed")); } catch (Exception ignored) {}
        try { p.setConfirmedBy(rs.getString("confirmed_by")); } catch (Exception ignored) {}
        try {
            Timestamp ct = rs.getTimestamp("confirmed_at");
            if (ct != null) p.setConfirmedAt(ct.toLocalDateTime());
        } catch (Exception ignored) {}
        return p;
    }
    /** Mark a payment as confirmed (administrator approved/reviewed it). */
    public void confirm(String paymentId, String confirmedBy) throws SQLException {
        String sql = "UPDATE payments SET confirmed=1, confirmed_by=?, " +
                "confirmed_at=GETDATE() WHERE payment_id=?";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, confirmedBy);
            ps.setString(2, paymentId);
            ps.executeUpdate();
        } finally { releaseConn(c); }
    }

    /** Unconfirm a payment — for dispute/manual review. */
    public void unconfirm(String paymentId) throws SQLException {
        String sql = "UPDATE payments SET confirmed=0, confirmed_by=NULL, " +
                "confirmed_at=NULL WHERE payment_id=?";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, paymentId);
            ps.executeUpdate();
        } finally { releaseConn(c); }
    }
    public void markRefunded(String paymentId, java.math.BigDecimal refundAmount) throws SQLException {
        String sql = "UPDATE payments SET status='REFUNDED_OVERPAY', " +
                "auto_refunded=1, refund_amount=? WHERE payment_id=?";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, refundAmount);
            ps.setString(2, paymentId);
            ps.executeUpdate();
        } finally { releaseConn(c); }
    }
    /** Reconciliation-side update — sets new status. */
    public void updateAfterMatch(String paymentId,
                                 PaymentStatus status) throws SQLException {
        String sql = "UPDATE payments SET status = ? WHERE payment_id = ?";
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, paymentId);
            ps.executeUpdate();
        } finally { releaseConn(conn); }
    }
    public int countByStatus(String vendorId, PaymentStatus status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM payments WHERE owner_id = ? AND status = ?";
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vendorId);
            ps.setString(2, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } finally { releaseConn(conn); }
        return 0;
    }
}