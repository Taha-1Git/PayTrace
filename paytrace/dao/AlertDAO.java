package com.paytrace.dao;

import com.paytrace.config.DatabaseConnection;
import com.paytrace.models.Alert;
import com.paytrace.models.enums.AlertStatus;
import com.paytrace.models.enums.AlertType;
import com.paytrace.models.enums.Severity;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AlertDAO {

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
    public void save(Alert alert) throws SQLException {
        String sql = "INSERT INTO alerts (alert_id, alert_type, severity, description, " +
                "status, age_in_days, entity_id, entity_type) " +
                "VALUES (NEWID(),?,?,?,?,?,?,?)";
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, alert.getAlertType().name());
            ps.setString(2, alert.getSeverity().name());
            ps.setString(3, alert.getDescription());
            ps.setString(4, AlertStatus.ACTIVE.name());
            ps.setInt(5, alert.getAgeInDays());
            ps.setString(6, alert.getEntityId());
            ps.setString(7, alert.getEntityType());
            ps.executeUpdate();
        } finally { releaseConn(conn); }
    }
    public List<Alert> findByStatus(AlertStatus status) throws SQLException {
        String sql = "SELECT * FROM alerts WHERE status = ? ORDER BY created_at DESC";
        List<Alert> list = new ArrayList<>();
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } finally { releaseConn(conn); }
        return list;
    }
    public List<Alert> findAll() throws SQLException {
        String sql = "SELECT * FROM alerts ORDER BY created_at DESC";
        List<Alert> list = new ArrayList<>();
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } finally { releaseConn(conn); }
        return list;
    }
    public void updateStatus(String alertId, AlertStatus status,
                             String resolvedBy) throws SQLException {
        String sql = "UPDATE alerts SET status=?, resolved_by=? WHERE alert_id=?";
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, resolvedBy);
            ps.setString(3, alertId);
            ps.executeUpdate();
        } finally { releaseConn(conn); }
    }

    private Alert mapRow(ResultSet rs) throws SQLException {
        Alert a = new Alert();
        a.setAlertId(rs.getString("alert_id"));
        a.setAlertType(AlertType.valueOf(rs.getString("alert_type")));
        a.setSeverity(Severity.valueOf(rs.getString("severity")));
        a.setDescription(rs.getString("description"));
        a.setStatus(AlertStatus.valueOf(rs.getString("status")));
        a.setAgeInDays(rs.getInt("age_in_days"));
        a.setResolvedBy(rs.getString("resolved_by"));
        a.setEntityId(rs.getString("entity_id"));
        a.setEntityType(rs.getString("entity_type"));
        return a;
    }
}