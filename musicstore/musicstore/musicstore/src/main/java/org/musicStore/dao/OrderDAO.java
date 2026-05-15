package org.musicStore.dao;

import org.musicStore.model.Order;
import org.musicStore.util.DBUtil;

import java.sql.*;
import java.util.*;
import java.util.function.Consumer;

public class OrderDAO {

    public static boolean isFirstOrder(int userId) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM `Order` WHERE userId = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static void getOrderHistory(int userId,
            Consumer<List<Order>> onSuccess,
            Consumer<Exception> onError) {

        DBUtil.DB_EXECUTOR.execute(() -> {
            try {
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM `Order` WHERE userId=? ORDER BY orderDate DESC, orderId DESC");

                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();

                List<Order> list = new ArrayList<>();

                while (rs.next()) {
                    Order o = new Order();
                    o.setOrderId(rs.getInt("orderId"));
                    o.setTotalAmount(rs.getDouble("totalAmount"));
                    o.setStatus(rs.getString("status"));
                    o.setOrderDate(rs.getDate("orderDate").toString());

                    // Fetch item names for this order
                    PreparedStatement itemPs = conn.prepareStatement(
                        "SELECT COALESCE(s.name, '[Deleted Product]') AS name, oi.quantity " +
                        "FROM OrderItem oi LEFT JOIN Stock s ON oi.productId = s.productId " +
                        "WHERE oi.orderId = ?");
                    itemPs.setInt(1, o.getOrderId());
                    ResultSet itemRs = itemPs.executeQuery();
                    StringBuilder itemNames = new StringBuilder();
                    while (itemRs.next()) {
                        if (itemNames.length() > 0) itemNames.append(", ");
                        itemNames.append(itemRs.getString("name"));
                        if (itemRs.getInt("quantity") > 1)
                            itemNames.append(" x").append(itemRs.getInt("quantity"));
                    }
                    o.setItemsSummary(itemNames.toString());
                    list.add(o);
                }

                onSuccess.accept(list);

            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }

    public static void updateOrderStatus(int orderId, String newStatus) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try {
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE `Order` SET status=? WHERE orderId=?");
                ps.setString(1, newStatus);
                ps.setInt(2, orderId);
                ps.executeUpdate();
                System.out.println("[OrderDAO] Order #" + orderId + " status → " + newStatus);
            } catch (Exception e) {
                System.err.println("[OrderDAO] Failed to update order status: " + e.getMessage());
            }
        });
    }

    public static void placeOrder(Order order, boolean withInsurance,
            Consumer<Integer> onSuccess,
            Consumer<Exception> onError) {

        DBUtil.DB_EXECUTOR.execute(() -> {
            Connection conn = null;
            try {
                conn = DBUtil.getConnection();
                conn.setAutoCommit(false);

                double total = order.getTotalAmount();

                if (withInsurance) {
                    total += total * 0.10;
                }

                String sql = "INSERT INTO `Order`(userId, totalAmount, status, orderDate) VALUES (?, ?, ?, NOW())";
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

                ps.setInt(1, order.getUserId());
                ps.setDouble(2, total);
                ps.setString(3, order.getStatus());

                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    int orderId = rs.getInt(1);

                    // Deduct stock; delete item if quantity reaches 0
                    if (order.getItems() != null) {
                        for (org.musicStore.model.OrderItem item : order.getItems()) {
                            // Deduct
                            String deductSql = "UPDATE stock SET quantity = quantity - ? WHERE productId = ? AND quantity >= ?";
                            try (PreparedStatement deductPs = conn.prepareStatement(deductSql)) {
                                deductPs.setInt(1, item.getQuantity());
                                deductPs.setInt(2, item.getProductId());
                                deductPs.setInt(3, item.getQuantity());
                                int updated = deductPs.executeUpdate();
                                if (updated == 0) {
                                    throw new Exception("Insufficient stock for product ID " + item.getProductId() + ". Please refresh your cart.");
                                }
                            }

                            // Insert OrderItem record (with vendorId snapshot for report integrity)
                            String oiSql = "INSERT INTO OrderItem(orderId, productId, quantity, price, vendorId) VALUES (?, ?, ?, ?, (SELECT vendorId FROM Stock WHERE productId = ?))";
                            try (PreparedStatement oiPs = conn.prepareStatement(oiSql)) {
                                oiPs.setInt(1, orderId);
                                oiPs.setInt(2, item.getProductId());
                                oiPs.setInt(3, item.getQuantity());
                                oiPs.setDouble(4, item.getPrice());
                                oiPs.setInt(5, item.getProductId());
                                oiPs.executeUpdate();
                            }
                        }
                    }

                    conn.commit();
                    onSuccess.accept(orderId);
                } else {
                    conn.rollback();
                    onError.accept(new Exception("Failed to create order."));
                }

            } catch (Exception e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
                onError.accept(e);
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}