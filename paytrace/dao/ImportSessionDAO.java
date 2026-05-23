package com.paytrace.dao;

import com.paytrace.config.DatabaseConnection;
import com.paytrace.models.ImportSession;
import com.paytrace.models.enums.ImportStatus;
import com.paytrace.models.enums.ImportType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImportSessionDAO {

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
    public void save(ImportSession s) throws SQLException {
        String sql = "INSERT INTO import_sessions (session_id, import_type, file_name, " +
                "imported_by, success_count, failure_count, status) " +
                "VALUES (NEWID(),?,?,?,?,?,?)";
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getImportType().name());
            ps.setString(2, s.getFileName());
            ps.setString(3, s.getImportedBy());
            ps.setInt(4, s.getSuccessCount());
            ps.setInt(5, s.getFailureCount());
            ps.setString(6, s.getStatus().name());
            ps.executeUpdate();
        } finally { releaseConn(conn); }
    }
    public Optional<ImportSession> findById(String sessionId) throws SQLException {
        String sql = "SELECT * FROM import_sessions WHERE session_id = ?";
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } finally { releaseConn(conn); }
        return Optional.empty();
    }
    public List<ImportSession> findByImportedBy(String userId) throws SQLException {
        String sql = "SELECT * FROM import_sessions WHERE imported_by = ? " +
                "ORDER BY imported_at DESC";
        List<ImportSession> list = new ArrayList<>();
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } finally { releaseConn(conn); }
        return list;
    }
    public void update(ImportSession s) throws SQLException {
        String sql = "UPDATE import_sessions SET success_count=?, failure_count=?, " +
                "status=? WHERE session_id=?";
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, s.getSuccessCount());
            ps.setInt(2, s.getFailureCount());
            ps.setString(3, s.getStatus().name());
            ps.setString(4, s.getSessionId());
            ps.executeUpdate();
        } finally { releaseConn(conn); }
    }

    private ImportSession mapRow(ResultSet rs) throws SQLException {
        ImportSession s = new ImportSession();
        s.setSessionId(rs.getString("session_id"));
        s.setImportType(ImportType.valueOf(rs.getString("import_type")));
        s.setFileName(rs.getString("file_name"));
        s.setImportedBy(rs.getString("imported_by"));
        s.setSuccessCount(rs.getInt("success_count"));
        s.setFailureCount(rs.getInt("failure_count"));
        s.setStatus(ImportStatus.valueOf(rs.getString("status")));
        return s;
    }
}