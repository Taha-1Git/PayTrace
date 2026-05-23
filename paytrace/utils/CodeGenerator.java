package com.paytrace.utils;

import com.paytrace.config.DatabaseConnection;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CodeGenerator {
    private static final SecureRandom RNG = new SecureRandom();

    /** 4-digit code unique across vendors. */
    public static String generateUnique4DigitCode() throws Exception {
        for (int i = 0; i < 50; i++) {
            String candidate = String.format("%04d", RNG.nextInt(10000));
            if (!exists("SELECT 1 FROM vendors WHERE access_code = ?", candidate))
                return candidate;
        }
        throw new RuntimeException("Could not generate unique 4-digit code.");
    }

    /** 10-digit vendor account number, prefixed with "ACC-". */
    public static String generateUniqueAccountNumber() throws Exception {
        for (int i = 0; i < 50; i++) {
            long n = (long) (RNG.nextDouble() * 10_000_000_000L);
            String candidate = "ACC-" + String.format("%010d", n);
            if (!exists("SELECT 1 FROM vendors WHERE account_number = ?", candidate))
                return candidate;
        }
        throw new RuntimeException("Could not generate unique account number.");
    }

    private static boolean exists(String sql, String param) throws SQLException {
        Connection c = null;
        try {
            c = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, param);
                try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
            }
        } catch (Exception e) {
            throw new SQLException(e.getMessage(), e);
        } finally {
            if (c != null) try { DatabaseConnection.getInstance().releaseConnection(c); }
            catch (Exception ignored) {}
        }
    }
}