package org.musicStore.dao;

import org.musicStore.util.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class CartDAO {

    public static void addItemToCart(int userId, int productId, int quantity) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try (Connection conn = DBUtil.getConnection()) {
                // استعلام بيدور على الـ Cart المربوط باليوزر ويضيف فيه المنتج
                // وإذا كان المنتج موجود أصلاً، بيزود الكمية (ON DUPLICATE KEY)
                String sql = "INSERT INTO CartItem (cartId, productId, quantity) " +
                        "SELECT cartId, ?, ? FROM Cart WHERE userId = ? " +
                        "ON DUPLICATE KEY UPDATE quantity = quantity + ?";

                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, productId);
                ps.setInt(2, quantity);
                ps.setInt(3, userId);
                ps.setInt(4, quantity);

                ps.executeUpdate();
                System.out.println("Item added to database cart!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
