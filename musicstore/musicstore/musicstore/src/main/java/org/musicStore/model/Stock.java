package org.musicStore.model;

public class Stock {
    private int productId;
    private String name;
    private String brand;
    private double price;
    private int quantity;
    private String category;
    private int vendorId;

    public Stock() {}

    public Stock(String name, String brand, double price, int quantity, String category) {
        if (price < 0) throw new IllegalArgumentException("Price cannot be negative.");
        if (quantity < 0) throw new IllegalArgumentException("Quantity cannot be negative.");
        this.name = name;
        this.brand = brand;
        this.price = price;
        this.quantity = quantity;
        this.category = category;
    }

    public void updateStock(int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Quantity cannot be negative.");
        this.quantity = quantity;
    }


    public void applyDiscount(double percentage) {
        if (percentage < 0 || percentage > 100)
            throw new IllegalArgumentException("Discount % must be 0-100.");
        this.price = this.price * (1 - percentage / 100.0);
    }


    public boolean isAvailable(int requestedQty) {
        return this.quantity >= requestedQty;
    }

    public int getProductId() { return productId; }
    public void setProductId(int id) { this.productId = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int q) { this.quantity = q; }
    public String getCategory() { return category; }
    public void setCategory(String c) { this.category = c; }

    public int getVendorId() { return vendorId; }
    public void setVendorId(int vendorId) { this.vendorId = vendorId; }

    @Override
    public String toString() {
        return "Stock{id=" + productId + ", name='" + name +
                "', price=" + price + ", qty=" + quantity + "}";
    }
}

