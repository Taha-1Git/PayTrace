package com.paytrace.dao;

import com.paytrace.config.DatabaseConnection;
import com.paytrace.models.Report;
import com.paytrace.models.enums.OutputFormat;
import com.paytrace.models.enums.ReportType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {

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
    public void save(Report r) throws SQLException {
        String sql = "INSERT INTO reports (report_id, report_type, generated_by, " +
                "output_format, date_range_from, date_range_to, generated_at, vendor_filter) " +
                "VALUES (NEWID(),?,?,?,?,?,GETDATE(),?)";
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getReportType().name());
            ps.setString(2, r.getGeneratedBy());
            ps.setString(3, r.getOutputFormat().name());
            ps.setObject(4, r.getDateRangeFrom());
            ps.setObject(5, r.getDateRangeTo());
            ps.setString(6, r.getVendorFilter());
            ps.executeUpdate();
        } finally { releaseConn(conn); }
    }
    public List<Report> findAll() throws SQLException {
        String sql = "SELECT * FROM reports ORDER BY generated_at DESC";
        List<Report> list = new ArrayList<>();
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } finally { releaseConn(conn); }
        return list;
    }
    public List<Report> findByGeneratedBy(String userId) throws SQLException {
        String sql = "SELECT * FROM reports WHERE generated_by = ? ORDER BY generated_at DESC";
        List<Report> list = new ArrayList<>();
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } finally { releaseConn(conn); }
        return list;
    }

    private Report mapRow(ResultSet rs) throws SQLException {
        Report r = new Report();
        r.setReportId(rs.getString("report_id"));
        r.setReportType(ReportType.valueOf(rs.getString("report_type")));
        r.setGeneratedBy(rs.getString("generated_by"));
        r.setOutputFormat(OutputFormat.valueOf(rs.getString("output_format")));
        r.setVendorFilter(rs.getString("vendor_filter"));
        return r;
    }
}