package com.paytrace.dao;

import com.paytrace.config.DatabaseConnection;
import com.paytrace.models.Vendor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VendorDAO {

    private Connection getConn() throws SQLException {
        try { return DatabaseConnection.getInstance().getConnection(); }
        catch (Exception e) { throw new SQLException(e.getMessage(), e); }
    }

    private void releaseConn(Connection c) {
        if (c != null) try { DatabaseConnection.getInstance().releaseConnection(c); }
        catch (Exception ignored) {}
    }
    public void save(Vendor v) throws SQLException {
        String sql = "INSERT INTO vendors (vendor_id, vendor_name, vendor_email, " +
                "vendor_address, access_code, account_number, administrator_id) " +
                "VALUES (NEWID(),?,?,?,?,?,?)";
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, v.getVendorName());
            ps.setString(2, v.getVendorEmail());
            ps.setString(3, v.getVendorAddress());
            ps.setString(4, v.getAccessCode());
            ps.setString(5, v.getAccountNumber());
            ps.setString(6, v.getAdministratorId());
            ps.executeUpdate();
        } finally { releaseConn(c); }
    }

     public Optional<Vendor> findById(String id) throws SQLException {
        return findOne("SELECT * FROM vendors WHERE vendor_id = ?", id);
    }

     public Optional<Vendor> findByCode(String code) throws SQLException {
        return findOne("SELECT * FROM vendors WHERE access_code = ?", code);
    }
     public Optional<Vendor> findByAdministratorId(String adminId) throws SQLException {
        return findOne("SELECT * FROM vendors WHERE administrator_id = ?", adminId);
    }

    // --- ADDED THIS METHOD TO MATCH INTERFACE ---
    public Optional<Vendor> findByNormalizedName(String normName) throws SQLException {
        return findOne("SELECT * FROM vendors WHERE vendor_name = ?", normName);
        // Note: Replace "vendor_name" with your actual column name for normalized names if applicable
    }

     public List<Vendor> findAll() throws SQLException {
        String sql = "SELECT * FROM vendors ORDER BY created_at DESC";
        List<Vendor> list = new ArrayList<>();
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } finally { releaseConn(c); }
        return list;
    }

    /** Keep as a helper or custom method since it's not in the interface provided */
    public Optional<Vendor> findByAccountNumber(String accountNumber) throws SQLException {
        return findOne("SELECT * FROM vendors WHERE account_number = ?", accountNumber);
    }

    private Optional<Vendor> findOne(String sql, String param) throws SQLException {
        Connection c = getConn();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } finally { releaseConn(c); }
        return Optional.empty();
    }

    private Vendor map(ResultSet rs) throws SQLException {
        Vendor v = new Vendor();
        v.setVendorId(rs.getString("vendor_id"));
        v.setVendorName(rs.getString("vendor_name"));
        v.setVendorEmail(rs.getString("vendor_email"));
        v.setVendorAddress(rs.getString("vendor_address"));
        v.setAccessCode(rs.getString("access_code"));
        v.setAccountNumber(rs.getString("account_number"));
        v.setAdministratorId(rs.getString("administrator_id"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) v.setCreatedAt(ts.toLocalDateTime());
        return v;
    }
}