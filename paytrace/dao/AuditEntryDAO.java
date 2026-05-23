package com.paytrace.dao;

import com.paytrace.config.DatabaseConnection;
import com.paytrace.models.AuditEntry;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditEntryDAO {

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
    public void save(AuditEntry e) throws SQLException {
        String sql = "INSERT INTO audit_entries (entry_id, event_type, performed_by, " +
                "performed_at, entity_type, entity_id, session_id, role_during_action) " +
                "VALUES (NEWID(),?,?,GETDATE(),?,?,?,?)";
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, e.getEventType());
            ps.setString(2, e.getPerformedBy());
            ps.setString(3, e.getEntityType());
            ps.setString(4, e.getEntityId());
            ps.setString(5, e.getSessionId());
            ps.setString(6, e.getRoleDuringAction());
            ps.executeUpdate();
        } finally { releaseConn(conn); }
    }
    public List<AuditEntry> findAll() throws SQLException {
        String sql = "SELECT * FROM audit_entries ORDER BY performed_at DESC";
        List<AuditEntry> list = new ArrayList<>();
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } finally { releaseConn(conn); }
        return list;
    }
    public List<AuditEntry> findByDateRange(LocalDateTime from,
                                            LocalDateTime to) throws SQLException {
        String sql = "SELECT * FROM audit_entries WHERE performed_at BETWEEN ? AND ? " +
                "ORDER BY performed_at DESC";
        List<AuditEntry> list = new ArrayList<>();
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } finally { releaseConn(conn); }
        return list;
    }
    public List<AuditEntry> findRecent(int limit) throws SQLException {
        String sql = "SELECT TOP (?) * FROM audit_entries ORDER BY performed_at DESC";
        List<AuditEntry> list = new ArrayList<>();
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } finally { releaseConn(conn); }
        return list;
    }

    private AuditEntry mapRow(ResultSet rs) throws SQLException {
        AuditEntry e = new AuditEntry();
        e.setEntryId(rs.getString("entry_id"));
        e.setEventType(rs.getString("event_type"));
        e.setPerformedBy(rs.getString("performed_by"));
        e.setPerformedAt(rs.getObject("performed_at", LocalDateTime.class));
        e.setEntityType(rs.getString("entity_type"));
        e.setEntityId(rs.getString("entity_id"));
        e.setSessionId(rs.getString("session_id"));
        e.setRoleDuringAction(rs.getString("role_during_action"));
        return e;
    }
}