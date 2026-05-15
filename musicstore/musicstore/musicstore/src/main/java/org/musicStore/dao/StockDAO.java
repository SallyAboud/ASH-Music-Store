package org.musicStore.dao;

import org.musicStore.model.Stock;
import org.musicStore.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class StockDAO {

    private static Stock mapRow(ResultSet rs) throws Exception {
        Stock s = new Stock(
                rs.getString("name"),
                rs.getString("brand"),
                rs.getDouble("price"),
                rs.getInt("quantity"),
                rs.getString("category"));
        s.setProductId(rs.getInt("productId"));
        try {
            s.setVendorId(rs.getInt("vendorId"));
        } catch (Exception ignored) {
        }
        return s;
    }

    // ── Get all products with qty > 0 ────────────────────────────────────────
    public static void getAllProducts(Consumer<List<Stock>> onSuccess, Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try (Connection conn = DBUtil.getConnection();
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM stock WHERE quantity > 0 AND deleted = 0")) {
                List<Stock> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                onSuccess.accept(list);
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }

    // ── Get all products including qty=0 (for manager/vendor management) ─────
    public static void getAllProductsIncludingEmpty(Consumer<List<Stock>> onSuccess, Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try (Connection conn = DBUtil.getConnection();
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM stock WHERE deleted = 0")) {
                List<Stock> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                onSuccess.accept(list);
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }

    // ── Get products by vendor (including qty=0 so vendor can manage) ─────────
    public static void getProductsByVendor(int vendorId, Consumer<List<Stock>> onSuccess, Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try (Connection conn = DBUtil.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM stock WHERE vendorId = ? AND deleted = 0")) {
                    ps.setInt(1, vendorId);
                    ResultSet rs = ps.executeQuery();
                    List<Stock> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                    onSuccess.accept(list);
                }
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }

    // ── Add product (manager/store) ───────────────────────────────────────────
    public static void addProduct(Stock product, Runnable onSuccess, Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try (Connection conn = DBUtil.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO stock(name, brand, price, quantity, category) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, product.getName());
                ps.setString(2, product.getBrand());
                ps.setDouble(3, product.getPrice());
                ps.setInt(4, product.getQuantity());
                ps.setString(5, product.getCategory());
                ps.executeUpdate();
                onSuccess.run();
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }

    // ── Add product for vendor ────────────────────────────────────────────────
    public static void addProductForVendor(Stock product, int vendorId, Runnable onSuccess,
            Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try (Connection conn = DBUtil.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO stock(name, brand, price, quantity, category, vendorId) VALUES (?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, product.getName());
                ps.setString(2, product.getBrand());
                ps.setDouble(3, product.getPrice());
                ps.setInt(4, product.getQuantity());
                ps.setString(5, product.getCategory());
                ps.setInt(6, vendorId);
                ps.executeUpdate();
                onSuccess.run();
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }

    // ── Full update (manager) ─────────────────────────────────────────────────
    public static void updateProduct(Stock stock, Runnable onSuccess, Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try (Connection conn = DBUtil.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE stock SET name = ?, brand = ?, price = ?, quantity = ?, category = ? WHERE productId = ?")) {
                ps.setString(1, stock.getName());
                ps.setString(2, stock.getBrand());
                ps.setDouble(3, stock.getPrice());
                ps.setInt(4, stock.getQuantity());
                ps.setString(5, stock.getCategory());
                ps.setInt(6, stock.getProductId());
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    onSuccess.run();
                } else {
                    onError.accept(new Exception("Product not found: " + stock.getProductId()));
                }
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }

    // ── Vendor: update quantity only (immediate) ──────────────────────────────
    public static void updateQuantityOnly(int productId, int newQty, Runnable onSuccess, Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try (Connection conn = DBUtil.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE stock SET quantity = ? WHERE productId = ?")) {
                ps.setInt(1, newQty);
                ps.setInt(2, productId);
                ps.executeUpdate();
                onSuccess.run();
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }

    // ── Search (qty > 0 only — for customers) ────────────────────────────────
    public static void searchByName(String keyword, Consumer<List<Stock>> onSuccess, Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try (Connection conn = DBUtil.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "SELECT * FROM stock WHERE (name LIKE ? OR brand LIKE ?) AND quantity > 0 AND deleted = 0")) {
                String p = "%" + keyword + "%";
                ps.setString(1, p);
                ps.setString(2, p);
                ResultSet rs = ps.executeQuery();
                List<Stock> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                onSuccess.accept(list);
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }

    // ── Search (for vendor — all qty) ────────────────────────────────────────
    public static void searchByNameForVendor(int vendorId, String keyword, Consumer<List<Stock>> onSuccess,
            Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try (Connection conn = DBUtil.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "SELECT * FROM stock WHERE vendorId = ? AND (name LIKE ? OR brand LIKE ?) AND deleted = 0")) {
                String p = "%" + keyword + "%";
                ps.setInt(1, vendorId);
                ps.setString(2, p);
                ps.setString(3, p);
                ResultSet rs = ps.executeQuery();
                List<Stock> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                onSuccess.accept(list);
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }

    // ── Delete (soft-delete: mark deleted=1, keep record for order history) ─────
    public static void deleteProduct(int id, Runnable onSuccess, Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try (Connection conn = DBUtil.getConnection();
                    PreparedStatement ps = conn.prepareStatement("UPDATE stock SET deleted=1, quantity=0 WHERE productId=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                onSuccess.run();
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }
}