package org.musicStore.util;

import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DBUtil {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/musicstore?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "S35@&Sql";

    public static final ExecutorService DB_EXECUTOR = Executors.newFixedThreadPool(4);

    // Auto-migrate: add vendorId snapshot column to OrderItem if it doesn't exist yet
    static {
        DB_EXECUTOR.execute(() -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Statement stmt = conn.createStatement()) {
                // Check if column exists
                try {
                    stmt.executeQuery("SELECT oi.vendorId FROM OrderItem oi LIMIT 1");
                } catch (SQLException missing) {
                    // Column doesn't exist — add it
                    stmt.executeUpdate("ALTER TABLE OrderItem ADD COLUMN vendorId INT NULL DEFAULT NULL");
                    // Back-fill existing rows from current Stock (best-effort for old orders)
                    stmt.executeUpdate(
                        "UPDATE OrderItem oi " +
                        "JOIN Stock s ON oi.productId = s.productId " +
                        "SET oi.vendorId = s.vendorId " +
                        "WHERE oi.vendorId IS NULL");
                }
            } catch (Exception e) {
                System.err.println("[DBUtil] Migration warning: " + e.getMessage());
            }
        });
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        conn.setAutoCommit(true);
        return conn;
    }

    public static void shutdown() {
        DB_EXECUTOR.shutdown();
    }
}
