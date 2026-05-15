package org.musicStore.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.musicStore.model.Customer;
import org.musicStore.model.CartItem;
import org.musicStore.Service.Sales;
import org.musicStore.util.EmailUtil;
import javafx.stage.Stage;
import javafx.application.Platform;
import java.util.List;

public class CheckoutController {

    @FXML private TextField nameField, emailField, phoneField;
    @FXML private TextArea  addressArea;
    @FXML private RadioButton cashRadio, cardRadio;
    @FXML private ToggleGroup paymentGroup;
    @FXML private Label totalLabel;
    @FXML private VBox  cardDetailsBox;
    @FXML private TextField cardNumberField, cardHolderField, cardExpiryField, cardCvvField;
    @FXML private Label   pointsInfoLabel;
    @FXML private javafx.scene.control.CheckBox usePointsCheckBox;
    @FXML private Label taxLabel;
    @FXML private Label subtotalLabel;
    @FXML private javafx.scene.control.CheckBox insuranceCheckBox;
    @FXML private Label insuranceLabel;

    private static final double TAX_RATE      = 0.14;
    private static final double INSURANCE_RATE = 0.10;
    private static final double FIRST_ORDER_DISCOUNT = 0.25;

    private Customer customer;
    private double   pointsDiscount = 0.0;
    private double   maxPointsDiscount = 0.0;
    private boolean  isFirstOrder = false;
    private final Sales salesService = new Sales();

    public void setCheckoutData(Customer c, double pointsDiscountAmount) {
        this.customer       = c;
        // pointsDiscountAmount passed in is the already-applied discount from profile; we ignore it here
        // and let the user choose in checkout

        nameField.setText(c.getUsername());
        if (emailField != null) emailField.setText(c.getEmail() != null ? c.getEmail() : "");
        phoneField.setText(c.getPhoneNumber() != null ? c.getPhoneNumber() : "");
        addressArea.setText(c.getAddress()     != null ? c.getAddress()     : "");

        int pts = c.getCustomerPoints();
        maxPointsDiscount = pts / 100.0;
        isFirstOrder = org.musicStore.dao.OrderDAO.isFirstOrder(c.getId());
        if (pointsInfoLabel != null) {
            String firstOrderNote = isFirstOrder ? "  🎉 25% First-Order Discount Applied!" : "";
            pointsInfoLabel.setText("Points: " + pts + "  (worth $" + String.format("%.2f", maxPointsDiscount) + ")" + firstOrderNote);
        }
        if (usePointsCheckBox != null) {
            usePointsCheckBox.setDisable(pts == 0);
            usePointsCheckBox.setSelected(pointsDiscountAmount > 0);
            usePointsCheckBox.setText("Apply all points for a $" + String.format("%.2f", maxPointsDiscount) + " discount");
            usePointsCheckBox.selectedProperty().addListener((obs, was, isNow) -> refreshTotal());
        }
        if (insuranceCheckBox != null) {
            insuranceCheckBox.selectedProperty().addListener((obs, was, isNow) -> refreshTotal());
        }
        refreshTotal();
        if (cardDetailsBox != null) { cardDetailsBox.setVisible(false); cardDetailsBox.setManaged(false); }
    }

    private void refreshTotal() {
        if (customer == null) return;
        double base = customer.getCart().calculateTotal();
        pointsDiscount = (usePointsCheckBox != null && usePointsCheckBox.isSelected()) ? maxPointsDiscount : 0.0;
        double afterPoints = Math.max(0, base - pointsDiscount);
        double firstOrderDiscountAmount = isFirstOrder ? afterPoints * FIRST_ORDER_DISCOUNT : 0.0;
        double discounted = Math.max(0, afterPoints - firstOrderDiscountAmount);
        double tax = discounted * TAX_RATE;
        boolean withInsurance = insuranceCheckBox != null && insuranceCheckBox.isSelected();
        double insuranceCost = withInsurance ? discounted * INSURANCE_RATE : 0.0;
        double grandTotal = discounted + tax + insuranceCost;
        if (subtotalLabel != null) {
            String discountNote = pointsDiscount > 0
                ? " (after $" + String.format("%.2f", pointsDiscount) + " points discount)" : "";
            if (isFirstOrder) discountNote += " 🎉 -25% first order!";
            subtotalLabel.setText("Subtotal: $" + String.format("%.2f", discounted) + discountNote);
        }
        if (taxLabel != null) taxLabel.setText("Tax (14%): $" + String.format("%.2f", tax));
        if (insuranceLabel != null) {
            if (withInsurance) {
                insuranceLabel.setText("Insurance (10%): $" + String.format("%.2f", insuranceCost));
                insuranceLabel.setVisible(true); insuranceLabel.setManaged(true);
            } else {
                insuranceLabel.setVisible(false); insuranceLabel.setManaged(false);
            }
        }
        if (totalLabel != null) totalLabel.setText("Total: $" + String.format("%.2f", grandTotal));
    }

    // Kept for backward compat
    public void setCheckoutData(Customer c) { setCheckoutData(c, 0.0); }

    // ===== EMAIL VALIDATION =====
    private boolean isEmailValid(String email) {
        return email.matches("^[A-Za-z0-9._%-]+@[A-Za-z0-9.-]+\\.(com|edu)$");
    }

    // FIX 3: phone must start with 01 + be 11 digits
    private boolean isPhoneValid(String phone) {
        return phone.matches("01\\d{9}");
    }

    @FXML public void initialize() {
        if (cardRadio != null && cardDetailsBox != null) {
            cardRadio.selectedProperty().addListener((obs, was, isNow) -> {
                cardDetailsBox.setVisible(isNow); cardDetailsBox.setManaged(isNow);
            });
        }
        if (cardNumberField != null) {
            cardNumberField.textProperty().addListener((obs, oldVal, newVal) -> {
                String digits = newVal.replaceAll("[^0-9]", "");
                if (digits.length() > 16) digits = digits.substring(0, 16);
                StringBuilder fmt = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    if (i > 0 && i % 4 == 0) fmt.append('-');
                    fmt.append(digits.charAt(i));
                }
                String result = fmt.toString();
                if (!result.equals(newVal)) { cardNumberField.setText(result); cardNumberField.positionCaret(result.length()); }
            });
        }
        if (cardExpiryField != null) {
            cardExpiryField.textProperty().addListener((obs, oldVal, newVal) -> {
                String digits = newVal.replaceAll("[^0-9]", "");
                if (digits.length() > 4) digits = digits.substring(0, 4);
                String result = digits.length() <= 2 ? digits : digits.substring(0,2) + "/" + digits.substring(2);
                if (!result.equals(newVal)) { cardExpiryField.setText(result); cardExpiryField.positionCaret(result.length()); }
            });
        }
    }

    @FXML private void confirmPurchase() {
        String name    = nameField.getText().trim();
        String email   = emailField  != null ? emailField.getText().trim() : "";
        String phone   = phoneField.getText().trim();
        String address = addressArea.getText().trim();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            showAlert("Missing Info", "Please fill in your name, phone, and address.", Alert.AlertType.WARNING); return;
        }
        if (emailField != null && !email.isEmpty() && !isEmailValid(email)) {
            showAlert("Invalid Email", "Email must be in the format: name@domain.com / .edu", Alert.AlertType.WARNING); return;
        }
        // FIX 3: phone must start with 01
        if (!isPhoneValid(phone)) {
            showAlert("Invalid Phone",
                "Phone number must start with 01 and be exactly 11 digits.",
                Alert.AlertType.WARNING);
            return;
        }
        if (cardRadio != null && cardRadio.isSelected()) {
            String cardNum    = cardNumberField  != null ? cardNumberField.getText().trim()  : "";
            String cardHolder = cardHolderField  != null ? cardHolderField.getText().trim()  : "";
            String expiry     = cardExpiryField  != null ? cardExpiryField.getText().trim()  : "";
            String cvv        = cardCvvField     != null ? cardCvvField.getText().trim()     : "";
            if (cardNum.isEmpty() || cardHolder.isEmpty() || expiry.isEmpty() || cvv.isEmpty()) {
                showAlert("Payment Info Required", "Please fill in all credit card details.", Alert.AlertType.WARNING); return;
            }
            if (!cardNum.replaceAll("-","").matches("\\d{13,16}")) {
                showAlert("Invalid Card Number", "Card number must be 13–16 digits.", Alert.AlertType.WARNING); return;
            }
            if (!expiry.matches("(0[1-9]|1[0-2])/\\d{2}")) {
                showAlert("Invalid Expiry", "Expiry date format must be MM/YY.", Alert.AlertType.WARNING); return;
            }
            if (!cvv.matches("\\d{3,4}")) {
                showAlert("Invalid CVV", "CVV must be 3 or 4 digits.", Alert.AlertType.WARNING); return;
            }
        }

        customer.setPhoneNumber(phone);
        customer.setAddress(address);

        String paymentMethod = (cashRadio != null && cashRadio.isSelected()) ? "Cash on Delivery" : "Credit Card";

        // Redeem points if checkbox was selected
        if (usePointsCheckBox != null && usePointsCheckBox.isSelected() && maxPointsDiscount > 0) {
            try { org.musicStore.dao.UserDAO.updatePoints(customer.getId(), 0); } catch (Exception ex) { ex.printStackTrace(); }
            customer.setCustomerPoints(0);
        }

        double base = customer.getCart().calculateTotal();
        double afterPoints = Math.max(0, base - pointsDiscount);
        double firstOrderDiscountAmount = isFirstOrder ? afterPoints * FIRST_ORDER_DISCOUNT : 0.0;
        double discounted = Math.max(0, afterPoints - firstOrderDiscountAmount);
        double tax = discounted * TAX_RATE;
        boolean withInsurance = insuranceCheckBox != null && insuranceCheckBox.isSelected();
        double insuranceCost = withInsurance ? discounted * INSURANCE_RATE : 0.0;
        double grandTotal = discounted + tax + insuranceCost;

        // Snapshot cart items BEFORE checkout clears the cart
        List<CartItem> cartSnapshot = new java.util.ArrayList<>(customer.getCart().getItems());

        salesService.checkout(customer, withInsurance, pointsDiscount,
            isFirstOrder ? FIRST_ORDER_DISCOUNT : 0.0,
            orderId -> Platform.runLater(() -> {
                // ── Update order status to CONFIRMED ──────────────────────────
                org.musicStore.dao.OrderDAO.updateOrderStatus(orderId, "CONFIRMED");

                String insuranceNote = withInsurance
                    ? "\nInsurance: $" + String.format("%.2f", insuranceCost) + " (10% coverage)" : "";
                String firstOrderNote = isFirstOrder ? "\n🎉 25% First-Order Discount Applied!" : "";
                showAlert("Order Placed!",
                    "Thank you! Order #" + orderId + " has been placed.\nPayment: " + paymentMethod
                    + firstOrderNote
                    + insuranceNote
                    + "\nTotal (incl. 14% tax): $" + String.format("%.2f", grandTotal),
                    Alert.AlertType.INFORMATION);

                // ── Send order-confirmation email ──────────────────────────────
                String recipientEmail = (emailField != null && !emailField.getText().trim().isEmpty())
                    ? emailField.getText().trim()
                    : customer.getEmail();
                if (recipientEmail != null && !recipientEmail.isEmpty()) {
                    int pointsEarned = (int)(grandTotal / 10);
                    EmailUtil.sendOrderConfirmation(
                        recipientEmail,
                        customer.getUsername(),
                        orderId,
                        cartSnapshot,
                        base,
                        pointsDiscount,
                        discounted * TAX_RATE,
                        insuranceCost,
                        grandTotal,
                        paymentMethod,
                        pointsEarned
                    );
                }

                customer.getCart().clearCart();
                closeWindow();
            }),
            e -> Platform.runLater(() -> showAlert("Order Failed", e.getMessage(), Alert.AlertType.ERROR))
        );
    }

    @FXML private void closeWindow() { ((Stage) nameField.getScene().getWindow()).close(); }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type); alert.setTitle(title); alert.setHeaderText(null);
        alert.setContentText(content); alert.showAndWait();
    }
}
