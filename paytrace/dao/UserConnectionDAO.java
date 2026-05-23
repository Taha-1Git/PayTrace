package com.paytrace.dao;

import com.paytrace.config.DatabaseConnection;
import com.paytrace.models.UserConnection;
import com.paytrace.models.enums.TargetType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserConnectionDAO {

    private Connection getConn() throws SQLException {
        try { return DatabaseConnection.getInstance().getConnection(); }
        catch (Exception e) { throw new SQLException(e.getMessage(), e); }
    }
    private void releaseConn(Connection c) {
        if (c != null) try { DatabaseConnection.getInstance().releaseConnection(c); }
        catch (Exception ignored) {}
    }
    public void save(UserConnection u) throws SQLException {
        String sql = "INSERT INTO user_connections " +
                "(connection_id, user_id, target_type, target_id, " +
                "access_code, user_account_number) " +
                "VALUES (NEWID(),?,?,?,?,?)";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getUserId());
            ps.setString(2, u.getTargetType().name());
            ps.setString(3, u.getTargetId());
            ps.setString(4, u.getAccessCode());
            ps.setString(5, u.getUserAccountNumber());   // may be null
            ps.executeUpdate();
        } finally { releaseConn(c); }
    }
    public List<UserConnection> findByUser(String userId) throws SQLException {
        String sql = "SELECT * FROM user_connections WHERE user_id=? ORDER BY granted_at DESC";
        List<UserConnection> out = new ArrayList<>();
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        } finally { releaseConn(c); }
        return out;
    }
    public Optional<UserConnection> findByUserAndCode(String userId, String code)
            throws SQLException {
        String sql = "SELECT * FROM user_connections WHERE user_id=? AND access_code=?";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } finally { releaseConn(c); }
        return Optional.empty();
    }
    public List<UserConnection> findByTarget(String targetId) throws SQLException {
        String sql = "SELECT * FROM user_connections WHERE target_id=? ORDER BY granted_at DESC";
        List<UserConnection> out = new ArrayList<>();
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, targetId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        } finally { releaseConn(c); }
        return out;
    }
    public Optional<UserConnection> findByUserAndTarget(String userId, String targetId)
            throws SQLException {
        String sql = "SELECT * FROM user_connections WHERE user_id=? AND target_id=?";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, targetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } finally { releaseConn(c); }
        return Optional.empty();
    }

    private UserConnection map(ResultSet rs) throws SQLException {
        UserConnection u = new UserConnection();
        u.setConnectionId(rs.getString("connection_id"));
        u.setUserId(rs.getString("user_id"));
        u.setTargetType(TargetType.valueOf(rs.getString("target_type")));
        u.setTargetId(rs.getString("target_id"));
        u.setAccessCode(rs.getString("access_code"));
        u.setUserAccountNumber(rs.getString("user_account_number"));
        Timestamp ts = rs.getTimestamp("granted_at");
        if (ts != null) u.setGrantedAt(ts.toLocalDateTime());
        return u;
    }
}