package org.musicStore.model;

public class OrderItem {
    private int orderItemId;
    private int orderId;
    private int productId;
    private int quantity;
    private double price;

    public OrderItem(int productId, int quantity, double price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    public double calculateSubtotal() {
        return price * quantity;
    }

    public int getOrderItemId() { return orderItemId; }
    public void setOrderItemId(int id) { this.orderItemId = id; }
    public int getOrderId() { return orderId; }
    public void setOrderId(int id) { this.orderId = id; }
    public int getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
}

