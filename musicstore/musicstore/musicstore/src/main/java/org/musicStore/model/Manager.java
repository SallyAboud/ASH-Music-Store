package org.musicStore.model;

public class Manager extends User {

    public Manager() { super(); }

    public void addProduct(Stock product) {
        if (product == null) throw new IllegalArgumentException("Product cannot be null.");
        System.out.println("[Manager] Adding product: " + product.getName());
    }


    public void deleteProduct(int productId) {
        if (productId <= 0) throw new IllegalArgumentException("Invalid product ID.");
        System.out.println("[Manager] Deleting product ID: " + productId);
    }

    public void updateProduct(Stock product) {
        if (product == null) throw new IllegalArgumentException("Product cannot be null.");
        System.out.println("[Manager] Updating product: " + product.getName());
    }


    public void generateReport() {
        System.out.println("[Manager] Generating report...");
    }


    public void updateOrderStatus(int orderId, String status) {
        if (status == null || status.isBlank())
            throw new IllegalArgumentException("Status cannot be empty.");
        System.out.println("[Manager] Order " + orderId + " status -> " + status);
    }


    public void setDiscount(int productId, double percentage) {
        if (percentage < 0 || percentage > 100)
            throw new IllegalArgumentException("Discount must be between 0 and 100.");
        System.out.println("[Manager] Set " + percentage + "% discount on product " + productId);
    }

    @Override
    public String toString() {
        return "Manager{" + super.toString() + "}";
    }
}

