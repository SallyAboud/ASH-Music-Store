package org.musicStore.model;

public class CartItem {
    private int productId;
    private String productName;
    private int quantity;
    private double unitPrice;

    public CartItem(int productId, String productName, int quantity, double unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getProductName() { return productName; }
    public int getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getSubtotal() { return unitPrice * quantity; }
    public void calculateSubtotal() { }
}
