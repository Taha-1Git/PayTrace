package com.paytrace.dao;

import com.paytrace.config.DatabaseConnection;
import com.paytrace.models.PaymentAllocation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaymentAllocationDAO {

    private Connection getConn() throws SQLException {
        try { return DatabaseConnection.getInstance().getConnection(); }
        catch (Exception e) { throw new SQLException(e.getMessage(), e); }
    }

    // Helper to safely release connections without throwing unhandled exceptions
    private void releaseConn(Connection conn) {
        if (conn != null) {
            try {
                DatabaseConnection.getInstance().releaseConnection(conn);
            } catch (Exception e) {
                System.err.println("Failed to release connection: " + e.getMessage());
            }
        }
    }
    public void save(PaymentAllocation a) throws SQLException {
        String sql = "INSERT INTO payment_allocations (allocation_id, payment_id, invoice_id, " +
                "allocated_amount, is_residual, source, allocation_date) " +
                "VALUES (NEWID(),?,?,?,?,?,GETDATE())";
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.getPaymentId());
            ps.setString(2, a.getInvoiceId());
            ps.setBigDecimal(3, a.getAllocatedAmount());
            ps.setBoolean(4, a.isResidual());
            ps.setString(5, a.getSource());
            ps.executeUpdate();
        } finally { releaseConn(conn); }
    }
    public List<PaymentAllocation> findByPaymentId(String paymentId) throws SQLException {
        String sql = "SELECT * FROM payment_allocations WHERE payment_id = ?";
        List<PaymentAllocation> list = new ArrayList<>();
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } finally { releaseConn(conn); }
        return list;
    }
    public List<PaymentAllocation> findByInvoiceId(String invoiceId) throws SQLException {
        String sql = "SELECT * FROM payment_allocations WHERE invoice_id = ?";
        List<PaymentAllocation> list = new ArrayList<>();
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } finally { releaseConn(conn); }
        return list;
    }

    private PaymentAllocation mapRow(ResultSet rs) throws SQLException {
        PaymentAllocation a = new PaymentAllocation();
        a.setAllocationId(rs.getString("allocation_id"));
        a.setPaymentId(rs.getString("payment_id"));
        a.setInvoiceId(rs.getString("invoice_id"));
        a.setAllocatedAmount(rs.getBigDecimal("allocated_amount"));
        a.setResidual(rs.getBoolean("is_residual"));
        a.setSource(rs.getString("source"));
        return a;
    }
}