package com.paytrace.dao;

import com.paytrace.config.DatabaseConnection;
import com.paytrace.models.ReconciliationRun;
import com.paytrace.models.enums.RunStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ReconciliationRunDAO {

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
    public void save(ReconciliationRun run) throws SQLException {
        String sql = "INSERT INTO reconciliation_runs (run_id, started_at, strategies_used, " +
                "fuzzy_threshold, total_matched, match_rate, initiated_by, status, " +
                "total_value_reconciled, tolerance_amount, tolerance_date_days) " +
                "VALUES (NEWID(),GETDATE(),?,?,?,?,?,?,?,?,?)";
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.join(",", run.getStrategiesUsed()));
            ps.setDouble(2, run.getFuzzyThreshold());
            ps.setInt(3, run.getTotalMatched());
            ps.setDouble(4, run.getMatchRate());
            ps.setString(5, run.getInitiatedBy());
            ps.setString(6, run.getStatus().name());
            ps.setBigDecimal(7, run.getTotalValueReconciled());
            ps.setBigDecimal(8, run.getToleranceAmount());
            ps.setInt(9, run.getToleranceDateDays());
            ps.executeUpdate();
        } finally { releaseConn(conn); }
    }
    public Optional<ReconciliationRun> findById(String runId) throws SQLException {
        String sql = "SELECT * FROM reconciliation_runs WHERE run_id = ?";
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } finally { releaseConn(conn); }
        return Optional.empty();
    }
    public List<ReconciliationRun> findAll() throws SQLException {
        String sql = "SELECT * FROM reconciliation_runs ORDER BY started_at DESC";
        List<ReconciliationRun> list = new ArrayList<>();
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } finally { releaseConn(conn); }
        return list;
    }
    public void update(ReconciliationRun run) throws SQLException {
        String sql = "UPDATE reconciliation_runs SET total_matched=?, match_rate=?, " +
                "status=?, total_value_reconciled=? WHERE run_id=?";
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, run.getTotalMatched());
            ps.setDouble(2, run.getMatchRate());
            ps.setString(3, run.getStatus().name());
            ps.setBigDecimal(4, run.getTotalValueReconciled());
            ps.setString(5, run.getRunId());
            ps.executeUpdate();
        } finally { releaseConn(conn); }
    }

    private ReconciliationRun mapRow(ResultSet rs) throws SQLException {
        ReconciliationRun r = new ReconciliationRun();
        r.setRunId(rs.getString("run_id"));
        r.setStrategiesUsed(Arrays.asList(
                rs.getString("strategies_used").split(",")));
        r.setFuzzyThreshold(rs.getDouble("fuzzy_threshold"));
        r.setTotalMatched(rs.getInt("total_matched"));
        r.setMatchRate(rs.getDouble("match_rate"));
        r.setInitiatedBy(rs.getString("initiated_by"));
        r.setStatus(RunStatus.valueOf(rs.getString("status")));
        r.setTotalValueReconciled(rs.getBigDecimal("total_value_reconciled"));
        r.setToleranceAmount(rs.getBigDecimal("tolerance_amount"));
        r.setToleranceDateDays(rs.getInt("tolerance_date_days"));
        return r;
    }
}