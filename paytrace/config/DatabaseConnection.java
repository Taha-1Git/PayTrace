package com.paytrace.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.LinkedList;
import java.util.Queue;


public class DatabaseConnection {

    private static DatabaseConnection instance;
    private final Properties props;
    private final Queue<Connection> connectionPool;
    private final int MAX_POOL_SIZE;

    private DatabaseConnection() throws IOException, ClassNotFoundException, SQLException {
        props = new Properties();
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in == null) throw new IOException("db.properties not found in classpath");
            props.load(in);
        }
        Class.forName(props.getProperty("db.driver"));
        MAX_POOL_SIZE = Integer.parseInt(
            props.getProperty("db.pool.maxSize", "10"));
        connectionPool = new LinkedList<>();
        initPool();
    }

    private void initPool() throws SQLException {
        int min = Integer.parseInt(props.getProperty("db.pool.minIdle", "2"));
        for (int i = 0; i < min; i++) {
            connectionPool.offer(createNewConnection());
        }
    }

    private Connection createNewConnection() throws SQLException {
        return DriverManager.getConnection(
            props.getProperty("db.url"),
            props.getProperty("db.username"),
            props.getProperty("db.password")
        );
    }

    /** Thread-safe singleton accessor */
    /** Thread-safe singleton accessor */
    public static synchronized DatabaseConnection getInstance()
            throws IOException, ClassNotFoundException, SQLException { // Added SQLException here
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /** Borrow a connection from the pool */
    public synchronized Connection getConnection() throws SQLException {
        if (!connectionPool.isEmpty()) {
            Connection conn = connectionPool.poll();
            if (conn != null && !conn.isClosed()) return conn;
        }
        if (connectionPool.size() < MAX_POOL_SIZE) {
            return createNewConnection();
        }
        throw new SQLException("Connection pool exhausted. Try again later.");
    }

    /** Return a connection back to the pool */
    public synchronized void releaseConnection(Connection conn) {
        if (conn != null) {
            connectionPool.offer(conn);
        }
    }

    /** Hard close all pooled connections (call on app shutdown) */
    public synchronized void shutdown() {
        for (Connection conn : connectionPool) {
            try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
        }
        connectionPool.clear();
        instance = null;
    }

    /** Quick connectivity test — called at app startup */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("[PayTrace] DB connection test FAILED: " + e.getMessage());
            return false;
        }
    }
}
