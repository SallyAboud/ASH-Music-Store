package org.musicStore.Service;

import org.musicStore.model.*;
import org.musicStore.model.Insurance;
import org.musicStore.dao.OrderDAO;
import java.util.function.Consumer;

public class Sales {

    public void generateSalesReport() { System.out.println("[Sales] Generating sales report..."); }
    public void viewOrders()          { System.out.println("[Sales] Viewing all orders..."); }

    public void applyTax(Order order) {
        if (order == null) throw new IllegalArgumentException("Order cannot be null.");
        order.setTotalAmount(order.getTotalAmount() * 1.14);
    }


    public void checkout(Customer customer, boolean withInsurance,
                         Consumer<Integer> onSuccess, Consumer<Exception> onError) {
        checkout(customer, withInsurance, 0.0, onSuccess, onError);
    }


    public void checkout(Customer customer, boolean withInsurance, double pointsDiscount,
                         Consumer<Integer> onSuccess, Consumer<Exception> onError) {
        checkout(customer, withInsurance, pointsDiscount, 0.0, onSuccess, onError);
    }

    
    public void checkout(Customer customer, boolean withInsurance, double pointsDiscount,
                         double pctDiscount,
                         Consumer<Integer> onSuccess, Consumer<Exception> onError) {
        Cart cart = customer.getCart();
        if (cart.getItems().isEmpty())
            throw new IllegalStateException("Cart is empty.");

        Order order = new Order(customer.getId());
        for (CartItem ci : cart.getItems()) {
            double unitPrice = ci.getQuantity() > 0 ? ci.getSubtotal() / ci.getQuantity() : 0;
            order.getItems().add(new OrderItem(ci.getProductId(), ci.getQuantity(), unitPrice));
        }
        order.calculateTotal();

        if (pointsDiscount > 0) {
            double discounted = Math.max(0, order.getTotalAmount() - pointsDiscount);
            order.setTotalAmount(discounted);
        }
        if (pctDiscount > 0) {
            order.setTotalAmount(order.getTotalAmount() * (1.0 - pctDiscount));
        }

        applyTax(order);

        if (withInsurance) {
            Insurance ins = new Insurance(0);
            double insBase = order.getTotalAmount() / 1.14; 
            double insCost = ins.calculateInsurance(insBase);
            order.setTotalAmount(order.getTotalAmount() + insCost);
        }

        int points = (int)(order.getTotalAmount() / 10);
        int newTotal = customer.getCustomerPoints() + points;
        customer.setCustomerPoints(newTotal);
        try { org.musicStore.dao.UserDAO.updatePoints(customer.getId(), newTotal); }
        catch (Exception e) { System.err.println("[Sales] Failed to save points: " + e.getMessage()); }

        OrderDAO.placeOrder(order, false, onSuccess, onError);
        cart.clearCart();
    }
}
