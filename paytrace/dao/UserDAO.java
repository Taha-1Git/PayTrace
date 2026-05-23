package com.paytrace.dao;

import com.paytrace.config.DatabaseConnection;
import com.paytrace.models.AdminUser;
import com.paytrace.models.AdministratorUser;
import com.paytrace.models.RegularUser;
import com.paytrace.models.User;
import com.paytrace.models.enums.AccountType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

    private Connection getConn() throws SQLException {
        try { return DatabaseConnection.getInstance().getConnection(); }
        catch (Exception e) { throw new SQLException(e.getMessage(), e); }
    }
    private void releaseConn(Connection conn) {
        if (conn != null) try { DatabaseConnection.getInstance().releaseConnection(conn); }
        catch (Exception ignored) {}
    }
    public void save(User u) throws SQLException {
        String sql = "INSERT INTO users (user_id, full_name, email, password_hash, " +
                "account_type, is_active) VALUES (NEWID(),?,?,?,?,1)";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPasswordHash());
            ps.setString(4, u.getAccountType().name());
            ps.executeUpdate();
        } finally { releaseConn(c); }
    }
    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } finally { releaseConn(c); }
        return Optional.empty();
    }
    public Optional<User> findById(String userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } finally { releaseConn(c); }
        return Optional.empty();
    }
    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        List<User> list = new ArrayList<>();
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } finally { releaseConn(c); }
        return list;
    }
    public void incrementFailedAttempts(String userId) throws SQLException {
        String sql = "UPDATE users SET failed_attempts = failed_attempts + 1 WHERE user_id=?";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId); ps.executeUpdate();
        } finally { releaseConn(c); }
    }
    public void resetFailedAttempts(String userId) throws SQLException {
        String sql = "UPDATE users SET failed_attempts = 0 WHERE user_id=?";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId); ps.executeUpdate();
        } finally { releaseConn(c); }
    }
    public void deactivate(String userId) throws SQLException {
        String sql = "UPDATE users SET is_active = 0 WHERE user_id=?";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId); ps.executeUpdate();
        } finally { releaseConn(c); }
    }

    private User map(ResultSet rs) throws SQLException {
        AccountType type = AccountType.valueOf(rs.getString("account_type"));

        // Polymorphic instantiation — DAO returns the right subclass for each role
        User u;
        switch (type) {
            case ADMIN:         u = new AdminUser();         break;
            case ADMINISTRATOR: u = new AdministratorUser(); break;
            case USER:          u = new RegularUser();       break;
            default:
                throw new SQLException("Unknown account type: " + type);
        }

        u.setUserId(rs.getString("user_id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setActive(rs.getBoolean("is_active"));
        try { u.setBlocked(rs.getBoolean("is_blocked")); } catch (Exception ignored) {}
        try { u.setBlockedReason(rs.getString("blocked_reason")); } catch (Exception ignored) {}
        try {
            Timestamp bt = rs.getTimestamp("blocked_at");
            if (bt != null) u.setBlockedAt(bt.toLocalDateTime());
        } catch (Exception ignored) {}
        u.setFailedAttempts(rs.getInt("failed_attempts"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) u.setCreatedAt(ts.toLocalDateTime());
        return u;
    }
    public void blockUser(String userId, String reason) throws SQLException {
        String sql = "UPDATE users SET is_blocked=1, blocked_reason=?, blocked_at=GETDATE() " +
                "WHERE user_id=?";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setString(2, userId);
            ps.executeUpdate();
        } finally { releaseConn(c); }
    }
}