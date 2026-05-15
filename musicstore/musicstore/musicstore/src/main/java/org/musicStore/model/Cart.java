package org.musicStore.model;

import java.util.ArrayList;
import java.util.List;

public class Cart {
    private int cartId;
    private int userId;
    private List<CartItem> items;
    private double totalAmount;

    public Cart(int userId) {
        this.userId = userId;
        this.items = new ArrayList<>();
        this.totalAmount = 0.0;
    }


    public void addItem(Stock product, int quantity) {
        if (product == null) throw new IllegalArgumentException("Product cannot be null.");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be > 0.");
        if (!product.isAvailable(quantity))
            throw new IllegalStateException("Insufficient stock for: " + product.getName());

        for (CartItem item : items) {
            if (item.getProductId() == product.getProductId()) {
                item.setQuantity(item.getQuantity() + quantity);
                calculateTotal();
                return;
            }
        }

        CartItem newItem = new CartItem(product.getProductId(), product.getName(), quantity, product.getPrice());
        items.add(newItem);
        calculateTotal();
    }

    public void removeItem(int productId) {
        boolean removed = items.removeIf(i -> i.getProductId() == productId);
        if (!removed) throw new IllegalArgumentException("Item not found in cart.");
        calculateTotal();
    }

    public void updateQuantity(int productId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be > 0.");
        for (CartItem item : items) {
            if (item.getProductId() == productId) {
                item.setQuantity(quantity);
                calculateTotal();
                return;
            }
        }
        throw new IllegalArgumentException("Product not found in cart.");
    }

    public double calculateTotal() {
        totalAmount = items.stream().mapToDouble(CartItem::getSubtotal).sum();
        return totalAmount;
    }

    public void clearCart() {
        items.clear();
        totalAmount = 0.0;
    }

    public int getCartId() { return cartId; }
    public void setCartId(int id) { this.cartId = id; }
    public int getUserId() { return userId; }
    public List<CartItem> getItems() { return items; }
    public double getTotalAmount() { return totalAmount; }
}


