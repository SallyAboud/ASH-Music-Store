package org.musicStore.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private int orderId;
    private int userId;
    private List<OrderItem> items;
    private double totalAmount;
    private String status;
    private String orderDate;
    private String itemsSummary = "";

    public Order() {
    }
    public Order(int userId) {
        this.userId = userId;
        this.items = new ArrayList<>();
        this.status = "PENDING";
        this.orderDate = LocalDate.now().toString();
    }

    public void confirmOrder() {
        if (!"PENDING".equals(status))
            throw new IllegalStateException("Only PENDING orders can be confirmed.");
        this.status = "CONFIRMED";
    }

    public void cancelOrder() {
        if ("DELIVERED".equals(status))
            throw new IllegalStateException("Cannot cancel a delivered order.");
        this.status = "CANCELLED";
    }

    public double calculateTotal() {
        totalAmount = items.stream().mapToDouble(OrderItem::calculateSubtotal).sum();
        return totalAmount;
    }

    public void updateStatus(String status) {
        if (status == null || status.isBlank())
            throw new IllegalArgumentException("Status cannot be blank.");
        this.status = status;
    }

    // Getters & Setters
    public int getOrderId() { return orderId; }
    public void setOrderId(int id) { this.orderId = id; }
    public int getUserId() { return userId; }
    public List<OrderItem> getItems() { return items; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double t) { this.totalAmount = t; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String d) { this.orderDate = d; }
    public String getItemsSummary() { return itemsSummary; }
    public void setItemsSummary(String s) { this.itemsSummary = s; }
}

