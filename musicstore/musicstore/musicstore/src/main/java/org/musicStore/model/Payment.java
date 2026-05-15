package org.musicStore.model;

import java.time.LocalDate;
public class Payment {
    private int paymentId;
    private int orderId;
    private double amount;
    private String paymentMethod;
    private String paymentStatus;
    private String paymentDate;

    public Payment(int paymentId) {
        this.paymentId = paymentId;
        this.paymentStatus = "PENDING";
        this.paymentDate = LocalDate.now().toString();
    }

    public void processPayment(int paymentId) {
        if (amount <= 0) throw new IllegalStateException("Amount must be greater than 0.");
        if (paymentMethod == null || paymentMethod.isBlank())
            throw new IllegalStateException("Payment method required.");
        this.paymentStatus = "COMPLETED";
        System.out.println("[Payment] Processed: $" + amount + " via " + paymentMethod);
    }
    public boolean confirmPayment() {
        return "COMPLETED".equals(this.paymentStatus);
    }
    public int getPaymentId() { return paymentId; }
    public int getOrderId() { return orderId; }
    public void setOrderId(int id) { this.orderId = id; }
    public double getAmount() { return amount; }
    public void setAmount(double a) { this.amount = a; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String m) { this.paymentMethod = m; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String s) { this.paymentStatus = s; }
    public String getPaymentDate() { return paymentDate; }
}

