package org.musicStore.model;

public class SalesReport extends Report {
    private double totalSales;
    private String topProduct;

    public SalesReport() { super(); }

    @Override
    public void generateReport() {
        System.out.println("=== SALES REPORT ===");
        System.out.println("Date: " + generatedDate);
        System.out.println("Total Sales: $" + totalSales);
        System.out.println("Top Product: " + topProduct);
    }

    public void calculateTotalSales() {
        System.out.println("[SalesReport] Calculating total sales from DB...");
    }

    public void listTopSellingProduct() {
        System.out.println("[SalesReport] Finding top-selling product...");
    }

    public double getTotalSales() { return totalSales; }
    public void setTotalSales(double t) { this.totalSales = t; }
    public String getTopProduct() { return topProduct; }
    public void setTopProduct(String p) { this.topProduct = p; }
}


