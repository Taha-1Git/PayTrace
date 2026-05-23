package com.paytrace.dao;

import com.paytrace.config.DatabaseConnection;
import com.paytrace.models.ReconciliationMatch;
import com.paytrace.models.enums.MatchType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReconciliationMatchDAO {

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
    public void save(ReconciliationMatch m) throws SQLException {
        String sql = "INSERT INTO reconciliation_matches (match_id, run_id, invoice_id, " +
                "payment_id, match_type, confidence_score, explanation, source, " +
                "is_reversed, matched_at, amount_difference) " +
                "VALUES (NEWID(),?,?,?,?,?,?,?,?,GETDATE(),?)";
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getRunId());
            ps.setString(2, m.getInvoiceId());
            ps.setString(3, m.getPaymentId());
            ps.setString(4, m.getMatchType().name());
            ps.setDouble(5, m.getConfidenceScore());
            ps.setString(6, m.getExplanation());
            ps.setString(7, m.getSource());
            ps.setBoolean(8, false);
            ps.setBigDecimal(9, m.getAmountDifference());
            ps.executeUpdate();
        } finally { releaseConn(conn); }
    }
    public List<ReconciliationMatch> findByRunId(String runId) throws SQLException {
        String sql = "SELECT * FROM reconciliation_matches WHERE run_id = ?";
        List<ReconciliationMatch> list = new ArrayList<>();
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } finally { releaseConn(conn); }
        return list;
    }
    public List<ReconciliationMatch> findByInvoiceId(String invoiceId) throws SQLException {
        String sql = "SELECT * FROM reconciliation_matches WHERE invoice_id = ?";
        List<ReconciliationMatch> list = new ArrayList<>();
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } finally { releaseConn(conn); }
        return list;
    }
    public void markReversed(String matchId) throws SQLException {
        String sql = "UPDATE reconciliation_matches SET is_reversed=1 WHERE match_id=?";
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matchId);
            ps.executeUpdate();
        } finally { releaseConn(conn); }
    }

    private ReconciliationMatch mapRow(ResultSet rs) throws SQLException {
        ReconciliationMatch m = new ReconciliationMatch();
        m.setMatchId(rs.getString("match_id"));
        m.setRunId(rs.getString("run_id"));
        m.setInvoiceId(rs.getString("invoice_id"));
        m.setPaymentId(rs.getString("payment_id"));
        m.setMatchType(MatchType.valueOf(rs.getString("match_type")));
        m.setConfidenceScore(rs.getDouble("confidence_score"));
        m.setExplanation(rs.getString("explanation"));
        m.setSource(rs.getString("source"));
        m.setReversed(rs.getBoolean("is_reversed"));
        m.setAmountDifference(rs.getBigDecimal("amount_difference"));
        return m;
    }
}