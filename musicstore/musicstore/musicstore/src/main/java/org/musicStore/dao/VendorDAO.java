package org.musicStore.dao;

import org.musicStore.model.Vendor;
import org.musicStore.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class VendorDAO {

    // ================= REGISTER VENDOR =================
    public static void registerVendor(String username, String email, String password,
            String vendorName, String phone,
            Runnable onSuccess, Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            Connection conn = null;
            try {
                conn = DBUtil.getConnection();
                conn.setAutoCommit(false);
                String hashedPassword = UserDAO.hashPassword(password);
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO User(username, email, password) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, username); ps.setString(2, email); ps.setString(3, hashedPassword);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) throw new Exception("Failed to get new user ID");
                int newId = keys.getInt(1);
                try (PreparedStatement vps = conn.prepareStatement(
                        "INSERT INTO Vendor(id, companyName, phoneNumber, approved) VALUES (?, ?, ?, false)")) {
                    vps.setInt(1, newId); vps.setString(2, vendorName);
                    if (phone == null || phone.isEmpty()) vps.setNull(3, java.sql.Types.VARCHAR);
                    else vps.setString(3, phone);
                    vps.executeUpdate();
                }
                conn.commit();
                onSuccess.run();
            } catch (Exception e) {
                if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } }
                e.printStackTrace();
                onError.accept(e);
            } finally {
                if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); } }
            }
        });
    }

    // ================= GET PENDING VENDORS =================
    public static void getPendingVendors(Consumer<List<Vendor>> onSuccess, Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try (Connection conn = DBUtil.getConnection()) {
                String sql = "SELECT u.id, u.username, u.email, v.companyName, v.phoneNumber, v.approved " +
                             "FROM User u JOIN Vendor v ON u.id = v.id WHERE v.approved = false";
                try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    List<Vendor> list = new ArrayList<>();
                    while (rs.next()) {
                        Vendor v = new Vendor();
                        v.setId(rs.getInt("id")); v.setUsername(rs.getString("username"));
                        v.setEmail(rs.getString("email")); v.setCompanyName(rs.getString("companyName"));
                        v.setPhoneNumber(rs.getString("phoneNumber")); v.setApproved(false);
                        list.add(v);
                    }
                    onSuccess.accept(list);
                }
            } catch (Exception e) { onError.accept(e); }
        });
    }

    // ================= GET APPROVED VENDORS =================
    public static void getApprovedVendors(Consumer<List<Vendor>> onSuccess, Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try (Connection conn = DBUtil.getConnection()) {
                String sql = "SELECT u.id, u.username, u.email, v.companyName, v.phoneNumber, v.approved " +
                             "FROM User u JOIN Vendor v ON u.id = v.id WHERE v.approved = true";
                try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    List<Vendor> list = new ArrayList<>();
                    while (rs.next()) {
                        Vendor v = new Vendor();
                        v.setId(rs.getInt("id")); v.setUsername(rs.getString("username"));
                        v.setEmail(rs.getString("email")); v.setCompanyName(rs.getString("companyName"));
                        v.setPhoneNumber(rs.getString("phoneNumber")); v.setApproved(true);
                        list.add(v);
                    }
                    onSuccess.accept(list);
                }
            } catch (Exception e) { onError.accept(e); }
        });
    }

    // ================= APPROVE VENDOR =================
    public static void approveVendor(int vendorId, Runnable onSuccess, Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE Vendor SET approved = true WHERE id = ?")) {
                ps.setInt(1, vendorId); ps.executeUpdate(); onSuccess.run();
            } catch (Exception e) { onError.accept(e); }
        });
    }

    // ================= REJECT VENDOR =================
    public static void rejectVendor(int vendorId, Runnable onSuccess, Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            Connection conn = null;
            try {
                conn = DBUtil.getConnection(); conn.setAutoCommit(false);
                try (PreparedStatement ps1 = conn.prepareStatement("DELETE FROM Vendor WHERE id = ?")) { ps1.setInt(1, vendorId); ps1.executeUpdate(); }
                try (PreparedStatement ps2 = conn.prepareStatement("DELETE FROM User   WHERE id = ?")) { ps2.setInt(1, vendorId); ps2.executeUpdate(); }
                conn.commit(); onSuccess.run();
            } catch (Exception e) {
                if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } }
                onError.accept(e);
            } finally {
                if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); } }
            }
        });
    }

    // getVendorSalesReport: includes deleted products; total price; gross revenue = 0.7 * total price
    public static void getVendorSalesReport(int vendorId, Consumer<List<String[]>> onSuccess,
            Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try (Connection conn = DBUtil.getConnection()) {
                // Use oi.vendorId snapshot; COALESCE with s.vendorId for old orders placed before migration
                String sql = "SELECT COALESCE(s.name, CONCAT('Product #', oi.productId)) AS name, " +
                             "COALESCE(s.brand, 'Unknown') AS brand, " +
                             "COALESCE(s.category, 'Unknown') AS category, " +
                             "oi.price AS price, " +
                             "SUM(oi.quantity) AS totalSold, " +
                             "SUM(oi.quantity * oi.price) AS totalPrice " +
                             "FROM OrderItem oi " +
                             "LEFT JOIN Stock s ON oi.productId = s.productId " +
                             "JOIN `Order` o ON oi.orderId = o.orderId " +
                             "WHERE COALESCE(oi.vendorId, s.vendorId) = ? " +
                             "AND o.status != 'cancelled' " +
                             "GROUP BY oi.productId, s.name, s.brand, s.category, oi.price " +
                             "ORDER BY totalSold DESC";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, vendorId);
                    ResultSet rs = ps.executeQuery();
                    List<String[]> rows = new ArrayList<>();
                    while (rs.next()) {
                        double tp = rs.getDouble("totalPrice");
                        double grossRev = tp * 0.70; // gross revenue = 70% of total price
                        double vendorShare = grossRev * 0.70; // vendor gets 70% of gross revenue
                        rows.add(new String[]{
                            rs.getString("name"), rs.getString("brand"), rs.getString("category"),
                            String.format("$%.2f", rs.getDouble("price")),
                            String.valueOf(rs.getInt("totalSold")),
                            String.format("$%.2f", tp),
                            String.format("$%.2f (70%% of Gross)", vendorShare)
                        });
                    }
                    onSuccess.accept(rows);
                }
            } catch (Exception e) { onError.accept(e); }
        });
    }

    // getAllVendorsSalesReport: no vendor column; Music Store = 100%; vendor = 30%; includes deleted products; total price + gross rev
    public static void getAllVendorsSalesReport(Consumer<List<String[]>> onSuccess, Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try (Connection conn = DBUtil.getConnection()) {
                // LEFT JOIN Stock so deleted products still appear (using OrderItem.productId)
                // oi.vendorId = snapshot stored at sale time; COALESCE with s.vendorId for old orders
                String sql = "SELECT COALESCE(s.name, CONCAT('Product #', oi.productId)) AS name, " +
                             "COALESCE(s.brand, 'Unknown') AS brand, " +
                             "COALESCE(s.category, 'Unknown') AS category, " +
                             "oi.price AS price, " +
                             "COALESCE(oi.vendorId, s.vendorId) AS vendorId, " +
                             "SUM(oi.quantity) AS totalSold, " +
                             "SUM(oi.quantity * oi.price) AS totalPrice " +
                             "FROM OrderItem oi " +
                             "LEFT JOIN Stock s ON oi.productId = s.productId " +
                             "JOIN `Order` o ON oi.orderId = o.orderId " +
                             "WHERE o.status != 'cancelled' " +
                             "GROUP BY oi.productId, s.name, s.brand, s.category, oi.price, COALESCE(oi.vendorId, s.vendorId) " +
                             "ORDER BY totalSold DESC";
                try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    List<String[]> rows = new ArrayList<>();
                    while (rs.next()) {
                        double tp = rs.getDouble("totalPrice");
                        int vendorId = rs.getInt("vendorId");
                        boolean isMusicStore = rs.wasNull() || vendorId == 0;
                        // gross revenue = 0.7 * total price (for all products)
                        double grossRev = tp * 0.70;
                        // vendor's 70% = 0.7 * gross revenue; store's 30% = 0.3 * gross revenue
                        // Music Store own products: store keeps all of gross revenue
                        double storeShare = isMusicStore ? grossRev : grossRev * 0.30;
                        rows.add(new String[]{
                            rs.getString("name"), rs.getString("brand"), rs.getString("category"),
                            String.format("$%.2f", rs.getDouble("price")),
                            String.valueOf(rs.getInt("totalSold")),
                            String.format("$%.2f", tp),
                            String.format("$%.2f", grossRev),
                            String.format("$%.2f (store)", storeShare)
                        });
                    }
                    onSuccess.accept(rows);
                }
            } catch (Exception e) { onError.accept(e); }
        });
    }

    // FIX 6: daily profit data for graph (returns date -> profit map)
    public static void getDailyProfitForVendor(int vendorId,
            Consumer<Map<String, Double>> onSuccess, Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try (Connection conn = DBUtil.getConnection()) {
                // FIX: vendor profit = 70% of gross revenue; gross revenue = 70% of price => 0.49 * price
                // Use COALESCE(oi.vendorId, s.vendorId) to handle old orders and deleted products
                String sql = "SELECT DATE(o.orderDate) AS day, " +
                             "SUM(oi.quantity * oi.price * 0.49) AS profit " +
                             "FROM OrderItem oi " +
                             "LEFT JOIN Stock s ON oi.productId = s.productId " +
                             "JOIN `Order` o ON oi.orderId = o.orderId " +
                             "WHERE COALESCE(oi.vendorId, s.vendorId) = ? AND o.status != 'cancelled' " +
                             "GROUP BY DATE(o.orderDate) ORDER BY day ASC";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, vendorId);
                    ResultSet rs = ps.executeQuery();
                    Map<String, Double> data = new LinkedHashMap<>();
                    while (rs.next()) data.put(rs.getString("day"), rs.getDouble("profit"));
                    onSuccess.accept(data);
                }
            } catch (Exception e) { onError.accept(e); }
        });
    }

    // FIX 6: daily profit data for manager (all vendors combined, store keeps 30%)
    public static void getDailyProfitAllVendors(
            Consumer<Map<String, Double>> onSuccess, Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try (Connection conn = DBUtil.getConnection()) {
                // Manager profit: own products = 0.7 * price; vendor products = 0.3 * 0.7 = 0.21 * price
                // Use COALESCE(oi.vendorId, s.vendorId) to handle old orders and deleted products
                String sql = "SELECT DATE(o.orderDate) AS day, " +
                             "SUM(CASE WHEN COALESCE(oi.vendorId, s.vendorId) IS NULL " +
                             "         THEN oi.quantity * oi.price * 0.70 " +
                             "         ELSE oi.quantity * oi.price * 0.21 END) AS profit " +
                             "FROM OrderItem oi " +
                             "LEFT JOIN Stock s ON oi.productId = s.productId " +
                             "JOIN `Order` o ON oi.orderId = o.orderId " +
                             "WHERE o.status != 'cancelled' " +
                             "GROUP BY DATE(o.orderDate) ORDER BY day ASC";
                try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    Map<String, Double> data = new LinkedHashMap<>();
                    while (rs.next()) data.put(rs.getString("day"), rs.getDouble("profit"));
                    onSuccess.accept(data);
                }
            } catch (Exception e) { onError.accept(e); }
        });
    }
}
