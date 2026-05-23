package com.paytrace.dao;

import com.paytrace.config.DatabaseConnection;
import com.paytrace.models.Session;
import com.paytrace.models.enums.AccountType;

import java.io.IOException; // Added this import
import java.sql.*;
import java.util.Optional;

public class SessionDAO {

    // Added IOException, ClassNotFoundException to the signature
    private Connection getConn() throws SQLException, IOException, ClassNotFoundException {
        return DatabaseConnection.getInstance().getConnection();
    }
    public void save(Session session) throws SQLException {
        String sql = "INSERT INTO sessions (session_id, user_id, role, login_time, expiry_time, is_active, ip_address) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            Connection conn = getConn();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, session.getSessionId());
                ps.setString(2, session.getUserId());
                ps.setString(3, session.getRole().name());
                ps.setObject(4, session.getLoginTime());
                ps.setObject(5, session.getExpiryTime());
                ps.setBoolean(6, session.isActive());
                ps.setString(7, session.getIpAddress());
                ps.executeUpdate();
            } finally {
                DatabaseConnection.getInstance().releaseConnection(conn);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new SQLException("Connection error: " + e.getMessage(), e);
        }
    }
    public Optional<Session> findById(String sessionId) throws SQLException {
        String sql = "SELECT * FROM sessions WHERE session_id = ?";
        try {
            Connection conn = getConn();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, sessionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return Optional.of(mapRow(rs));
                }
            } finally {
                DatabaseConnection.getInstance().releaseConnection(conn);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new SQLException("Connection error: " + e.getMessage(), e);
        }
        return Optional.empty();
    }
    public void invalidate(String sessionId) throws SQLException {
        String sql = "UPDATE sessions SET is_active = 0 WHERE session_id = ?";
        try {
            Connection conn = getConn();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, sessionId);
                ps.executeUpdate();
            } finally {
                DatabaseConnection.getInstance().releaseConnection(conn);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new SQLException("Connection error: " + e.getMessage(), e);
        }
    }
    public void invalidateAllForUser(String userId) throws SQLException {
        String sql = "UPDATE sessions SET is_active = 0 WHERE user_id = ?";
        try {
            Connection conn = getConn();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, userId);
                ps.executeUpdate();
            } finally {
                DatabaseConnection.getInstance().releaseConnection(conn);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new SQLException("Connection error: " + e.getMessage(), e);
        }
    }

    private Session mapRow(ResultSet rs) throws SQLException {
        Session s = new Session();
        s.setSessionId(rs.getString("session_id"));
        s.setUserId(rs.getString("user_id"));
        s.setRole(AccountType.valueOf(rs.getString("role")));
        s.setLoginTime(rs.getObject("login_time", java.time.LocalDateTime.class));
        s.setExpiryTime(rs.getObject("expiry_time", java.time.LocalDateTime.class));
        s.setActive(rs.getBoolean("is_active"));
        s.setIpAddress(rs.getString("ip_address"));
        return s;
    }
}