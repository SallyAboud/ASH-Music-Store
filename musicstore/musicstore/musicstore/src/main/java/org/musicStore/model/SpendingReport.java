package org.musicStore.model;

public class SpendingReport extends Report {
    private double customerSpending;
    private int targetUserId;

    public SpendingReport() { super(); }

    @Override
    public void generateReport() {
        System.out.println("=== SPENDING REPORT ===");
        System.out.println("User ID: " + targetUserId);
        System.out.println("Total Spending: $" + customerSpending);
    }

    public void calculateCustomerSpending(int userId) {
        this.targetUserId = userId;
        System.out.println("[SpendingReport] Calculating spending for user " + userId);
    }

    public double getCustomerSpending() { return customerSpending; }
    public void setCustomerSpending(double s) { this.customerSpending = s; }
}

