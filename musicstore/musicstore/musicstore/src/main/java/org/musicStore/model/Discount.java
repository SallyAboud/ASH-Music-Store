package org.musicStore.model;

public class Discount {
    private int discountId;
    private double percentage;
    private boolean active;

    public Discount(int discountId, double percentage) {
        if (percentage < 0 || percentage > 100)
            throw new IllegalArgumentException("Discount % must be 0-100.");
        this.discountId = discountId;
        this.percentage = percentage;
        this.active = true;
    }


    public double applyDiscount(double price) {
        if (!active) throw new IllegalStateException("Discount is not active.");
        if (price < 0) throw new IllegalArgumentException("Price cannot be negative.");
        return price * (1 - percentage / 100.0);
    }


    public int getDiscountId() { return discountId; }
    public double getPercentage() { return percentage; }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }
}

