package com.paytrace.dao;

import com.paytrace.config.DatabaseConnection;
import com.paytrace.models.ConnectionRequest;
import com.paytrace.models.enums.RequestStatus;
import com.paytrace.models.enums.TargetType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConnectionRequestDAO {

    private Connection getConn() throws SQLException {
        try { return DatabaseConnection.getInstance().getConnection(); }
        catch (Exception e) { throw new SQLException(e.getMessage(), e); }
    }
    private void releaseConn(Connection c) {
        if (c != null) try { DatabaseConnection.getInstance().releaseConnection(c); }
        catch (Exception ignored) {}
    }
    public void save(ConnectionRequest r) throws SQLException {
        String sql = "INSERT INTO connection_requests (request_id, user_id, target_type, " +
                "target_id, user_address, user_job, user_phone, extra_info, " +
                "user_account_number, status) " +
                "VALUES (NEWID(),?,?,?,?,?,?,?,?,'PENDING')";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getUserId());
            ps.setString(2, r.getTargetType().name());
            ps.setString(3, r.getTargetId());
            ps.setString(4, r.getUserAddress());
            ps.setString(5, r.getUserJob());
            ps.setString(6, r.getUserPhone());
            ps.setString(7, r.getExtraInfo());
            ps.setString(8, r.getUserAccountNumber());
            ps.executeUpdate();
        } finally { releaseConn(c); }
    }
    public List<ConnectionRequest> findByTarget(String targetId) throws SQLException {
        String sql = "SELECT * FROM connection_requests WHERE target_id=? ORDER BY requested_at DESC";
        return query(sql, targetId);
    }
    public List<ConnectionRequest> findByUser(String userId) throws SQLException {
        String sql = "SELECT * FROM connection_requests WHERE user_id=? ORDER BY requested_at DESC";
        return query(sql, userId);
    }
    public void updateStatus(String requestId, RequestStatus status, String decidedBy)
            throws SQLException {
        String sql = "UPDATE connection_requests SET status=?, decided_at=GETDATE(), decided_by=? " +
                "WHERE request_id=?";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, decidedBy);
            ps.setString(3, requestId);
            ps.executeUpdate();
        } finally { releaseConn(c); }
    }

    private List<ConnectionRequest> query(String sql, String param) throws SQLException {
        List<ConnectionRequest> out = new ArrayList<>();
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        } finally { releaseConn(c); }
        return out;
    }

    private ConnectionRequest map(ResultSet rs) throws SQLException {
        ConnectionRequest r = new ConnectionRequest();
        r.setRequestId(rs.getString("request_id"));
        r.setUserId(rs.getString("user_id"));
        r.setTargetType(TargetType.valueOf(rs.getString("target_type")));
        r.setTargetId(rs.getString("target_id"));
        r.setUserAddress(rs.getString("user_address"));
        r.setUserJob(rs.getString("user_job"));
        r.setUserPhone(rs.getString("user_phone"));
        r.setExtraInfo(rs.getString("extra_info"));
        r.setStatus(RequestStatus.valueOf(rs.getString("status")));
        Timestamp t1 = rs.getTimestamp("requested_at");
        if (t1 != null) r.setRequestedAt(t1.toLocalDateTime());
        Timestamp t2 = rs.getTimestamp("decided_at");
        if (t2 != null) r.setDecidedAt(t2.toLocalDateTime());
        r.setDecidedBy(rs.getString("decided_by"));
        r.setUserAccountNumber(rs.getString("user_account_number"));
        return r;
    }
    public java.util.Optional<ConnectionRequest> findById(String requestId)
            throws java.sql.SQLException {
        String sql = "SELECT * FROM connection_requests WHERE request_id = ?";
        java.sql.Connection c = getConn();
        try (java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, requestId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return java.util.Optional.of(map(rs));
            }
        } finally { releaseConn(c); }
        return java.util.Optional.empty();
    }
}