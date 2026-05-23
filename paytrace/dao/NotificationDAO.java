package com.paytrace.dao;

import com.paytrace.config.DatabaseConnection;
import com.paytrace.models.Notification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    private Connection getConn() throws SQLException {
        try { return DatabaseConnection.getInstance().getConnection(); }
        catch (Exception e) { throw new SQLException(e.getMessage(), e); }
    }
    private void releaseConn(Connection c) {
        if (c != null) try { DatabaseConnection.getInstance().releaseConnection(c); }
        catch (Exception ignored) {}
    }
    public void save(Notification n) throws SQLException {
        String sql = "INSERT INTO notifications (notification_id, recipient_user_id, " +
                "notification_type, title, message, related_entity_type, " +
                "related_entity_id, is_read) VALUES (NEWID(),?,?,?,?,?,?,0)";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, n.getRecipientUserId());
            ps.setString(2, n.getType() != null ? n.getType().name()
                    : Notification.Type.GENERIC.name());
            ps.setString(3, n.getTitle());
            ps.setString(4, n.getMessage());
            ps.setString(5, n.getRelatedEntityType());
            ps.setString(6, n.getRelatedEntityId());
            ps.executeUpdate();
        } finally { releaseConn(c); }
    }
    public List<Notification> findByRecipient(String userId) throws SQLException {
        return many("SELECT * FROM notifications WHERE recipient_user_id = ? " +
                "ORDER BY created_at DESC", userId);
    }
    public List<Notification> findUnread(String userId) throws SQLException {
        return many("SELECT * FROM notifications WHERE recipient_user_id = ? " +
                "AND is_read = 0 ORDER BY created_at DESC", userId);
    }
    public List<Notification> findRecent(String userId, int limit) throws SQLException {
        String sql = "SELECT TOP (?) * FROM notifications WHERE recipient_user_id = ? " +
                "ORDER BY created_at DESC";
        List<Notification> list = new ArrayList<>();
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } finally { releaseConn(c); }
        return list;
    }
    public int countUnread(String userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications " +
                "WHERE recipient_user_id = ? AND is_read = 0";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } finally { releaseConn(c); }
        return 0;
    }
    public void markAsRead(String notificationId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = 1, read_at = GETDATE() " +
                "WHERE notification_id = ?";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, notificationId);
            ps.executeUpdate();
        } finally { releaseConn(c); }
    }
    public void markAllAsRead(String userId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = 1, read_at = GETDATE() " +
                "WHERE recipient_user_id = ? AND is_read = 0";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.executeUpdate();
        } finally { releaseConn(c); }
    }

    private List<Notification> many(String sql, String param) throws SQLException {
        List<Notification> list = new ArrayList<>();
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } finally { releaseConn(c); }
        return list;
    }

    private Notification map(ResultSet rs) throws SQLException {
        Notification n = new com.paytrace.models.InAppNotification();
        n.setNotificationId(rs.getString("notification_id"));
        n.setRecipientUserId(rs.getString("recipient_user_id"));
        n.setType(Notification.Type.valueOf(rs.getString("notification_type")));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setRelatedEntityType(rs.getString("related_entity_type"));
        n.setRelatedEntityId(rs.getString("related_entity_id"));
        n.setRead(rs.getBoolean("is_read"));
        Timestamp t1 = rs.getTimestamp("created_at");
        if (t1 != null) n.setCreatedAt(t1.toLocalDateTime());
        Timestamp t2 = rs.getTimestamp("read_at");
        if (t2 != null) n.setReadAt(t2.toLocalDateTime());
        return n;
    }
}