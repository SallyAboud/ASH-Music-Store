package org.musicStore.gui;

import org.musicStore.dao.*;
import org.musicStore.model.*;
import org.musicStore.util.ProductImageUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.application.Platform;
import java.util.List;
import java.util.ArrayList;

public class CustomerDashboard {

    @FXML private FlowPane productGrid;
    @FXML private ScrollPane productScrollPane;
    @FXML private TextField searchField;
    @FXML private TextField filterBrandField, filterCategoryField, filterMinPriceField, filterMaxPriceField;
    @FXML private ListView<String> cartListView;
    @FXML private Label totalLabel, pointsLabel, statusLabel, customerNameLabel;
    @FXML private Button backButton;

    private Customer customer;
    private double pointsDiscountAmount = 0.0;
    private List<Stock> currentProducts = new ArrayList<>();
    private Stock selectedProduct = null;

    public void setCustomer(Customer c) {
        this.customer = c;
        if (this.customer.getCart() == null)
            this.customer.setCart(new Cart(this.customer.getId()));
        pointsLabel.setText("Points: " + c.getCustomerPoints());
        if (customerNameLabel != null) customerNameLabel.setText(c.getUsername());
        loadProducts();
        if (backButton != null) { backButton.setVisible(false); backButton.setManaged(false); }
        updateCartView();
    }

    @FXML
    public void initialize() {
        if (productGrid != null && productScrollPane != null) {
            productGrid.prefWidthProperty().bind(productScrollPane.widthProperty().subtract(20));
        }
    }

    private void loadProducts() {
        if (backButton != null) { backButton.setVisible(false); backButton.setManaged(false); }
        StockDAO.getAllProducts(
            products -> Platform.runLater(() -> { currentProducts = products; renderProductCards(products); }),
            e -> Platform.runLater(() -> statusLabel.setText("Error: " + e.getMessage())));
    }

    private void renderProductCards(List<Stock> products) {
        productGrid.getChildren().clear();
        selectedProduct = null;
        for (Stock s : products) productGrid.getChildren().add(createProductCard(s));
    }

    private VBox createProductCard(Stock s) {
        VBox card = new VBox(8);
        card.setPrefWidth(195);
        card.setPrefHeight(245);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: #1e1035;-fx-border-color: rgba(255,255,255,0.08);-fx-border-radius: 14;-fx-background-radius: 14;-fx-border-width: 1;-fx-cursor: hand;");

        String badge; String badgeColor;
        if (s.getQuantity() <= 0) { badge = "Out of Stock"; badgeColor = "#e74c3c"; }
        else if (s.getQuantity() < 5) { badge = "Low Stock"; badgeColor = "#e67e22"; }
        else { badge = "In Stock"; badgeColor = "#3ddc84"; }
        HBox badgeRow = new HBox();
        badgeRow.setAlignment(Pos.TOP_RIGHT);
        Label badgeLbl = new Label(badge);
        badgeLbl.setStyle("-fx-background-color: " + badgeColor + ";-fx-text-fill: #0a0a0f;-fx-font-size: 10px;-fx-font-weight: bold;-fx-background-radius: 20;-fx-padding: 3 9;");
        badgeRow.getChildren().add(badgeLbl);

        String emoji = ProductImageUtil.getEmoji(s);
        String color = ProductImageUtil.getCellColor(s);
        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(70, 70);
        Rectangle bg = new Rectangle(70, 70);
        bg.setArcWidth(14); bg.setArcHeight(14);
        bg.setStyle("-fx-fill: " + color + ";");
        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 32px;");
        iconPane.getChildren().addAll(bg, emojiLbl);
        iconPane.setAlignment(Pos.CENTER);
        VBox.setMargin(iconPane, new Insets(2, 0, 4, 0));

        Label nameLbl = new Label(s.getName());
        nameLbl.setStyle("-fx-font-size: 13px;-fx-font-weight: bold;-fx-text-fill: #e8e8f0;");
        nameLbl.setWrapText(true);

        Label subLbl = new Label(s.getBrand() + " • " + s.getCategory());
        subLbl.setStyle("-fx-font-size: 11px;-fx-text-fill: #6b6b85;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label priceLbl = new Label(String.format("$%,.0f", s.getPrice()));
        priceLbl.setStyle("-fx-font-size: 17px;-fx-font-weight: bold;-fx-text-fill: #c9a84c;");

        card.getChildren().addAll(badgeRow, iconPane, nameLbl, subLbl, spacer, priceLbl);

        card.setOnMouseClicked(e -> {
            productGrid.getChildren().forEach(node -> { if (node instanceof VBox) ((VBox) node).setStyle("-fx-background-color: #1e1035;-fx-border-color: rgba(255,255,255,0.08);-fx-border-radius: 14;-fx-background-radius: 14;-fx-border-width: 1;-fx-cursor: hand;"); });
            card.setStyle("-fx-background-color: #2a1a4a;-fx-border-color: #c9a84c;-fx-border-radius: 14;-fx-background-radius: 14;-fx-border-width: 2;-fx-cursor: hand;");
            selectedProduct = s;
            statusLabel.setText("Selected: " + s.getName());
        });
        card.setOnMouseEntered(e -> { if (selectedProduct != s) card.setStyle("-fx-background-color: #251540;-fx-border-color: rgba(201,168,76,0.4);-fx-border-radius: 14;-fx-background-radius: 14;-fx-border-width: 1;-fx-cursor: hand;"); });
        card.setOnMouseExited(e -> { if (selectedProduct != s) card.setStyle("-fx-background-color: #1e1035;-fx-border-color: rgba(255,255,255,0.08);-fx-border-radius: 14;-fx-background-radius: 14;-fx-border-width: 1;-fx-cursor: hand;"); });
        return card;
    }

    @FXML private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) { loadProducts(); return; }
        if (backButton != null) { backButton.setVisible(true); backButton.setManaged(true); }
        StockDAO.searchByName(keyword,
            results -> Platform.runLater(() -> { currentProducts = results; renderProductCards(results); statusLabel.setText("Results for: \"" + keyword + "\""); }),
            e -> Platform.runLater(() -> statusLabel.setText("Search error: " + e.getMessage())));
    }

    @FXML private void handleBack() { searchField.clear(); loadProducts(); statusLabel.setText("Showing all products."); }

    @FXML private void handleFilter() {
        String brand    = filterBrandField    != null ? filterBrandField.getText().trim().toLowerCase() : "";
        String category = filterCategoryField != null ? filterCategoryField.getText().trim().toLowerCase() : "";
        String minStr   = filterMinPriceField != null ? filterMinPriceField.getText().trim() : "";
        String maxStr   = filterMaxPriceField != null ? filterMaxPriceField.getText().trim() : "";
        StockDAO.getAllProducts(products -> {
            List<Stock> filtered = products.stream().filter(s -> {
                if (!brand.isEmpty()    && !s.getBrand().toLowerCase().contains(brand))    return false;
                if (!category.isEmpty() && !s.getCategory().toLowerCase().contains(category)) return false;
                if (!minStr.isEmpty()) { try { if (s.getPrice() < Double.parseDouble(minStr)) return false; } catch (NumberFormatException ignored) {} }
                if (!maxStr.isEmpty()) { try { if (s.getPrice() > Double.parseDouble(maxStr)) return false; } catch (NumberFormatException ignored) {} }
                return true;
            }).collect(java.util.stream.Collectors.toList());
            Platform.runLater(() -> { currentProducts = filtered; renderProductCards(filtered); statusLabel.setText("Filtered: " + filtered.size() + " products found."); });
        }, e -> Platform.runLater(() -> statusLabel.setText("Filter error: " + e.getMessage())));
    }

    @FXML private void handleAddToCart() {
        if (selectedProduct == null) { statusLabel.setText("Please click a product card first."); return; }
        Stock selected = selectedProduct;
        if (selected.getQuantity() <= 0) { showAlert("Sold Out", "This item is currently unavailable.", Alert.AlertType.ERROR); return; }
        int alreadyInCartTmp = 0;
        for (CartItem ci : customer.getCart().getItems()) { if (ci.getProductId() == selected.getProductId()) { alreadyInCartTmp = ci.getQuantity(); break; } }
        final int alreadyInCart = alreadyInCartTmp;
        final int maxAllowed = selected.getQuantity() - alreadyInCart;
        if (maxAllowed <= 0) { showAlert("Stock Limit", "You already have all available stock in your cart.", Alert.AlertType.WARNING); return; }
        TextInputDialog dlg = new TextInputDialog("1");
        dlg.setTitle("Select Quantity"); dlg.setHeaderText("Add to Cart: " + selected.getName()); dlg.setContentText("Enter quantity (Max available: " + maxAllowed + "):");
        dlg.showAndWait().ifPresent(s -> {
            try {
                int qty = Integer.parseInt(s);
                if (qty <= 0) { statusLabel.setText("Quantity must be positive."); }
                else if (qty > maxAllowed) { showAlert("Insufficient Stock", "Only " + maxAllowed + " more unit(s) available (you already have " + alreadyInCart + " in cart).", Alert.AlertType.WARNING); }
                else { customer.getCart().addItem(selected, qty); updateCartView(); statusLabel.setText("Added " + qty + " x " + selected.getName()); }
            } catch (NumberFormatException e) { statusLabel.setText("Invalid number."); }
        });
    }

    @FXML private void handleEditCart() {
        if (customer.getCart().getItems().isEmpty()) { statusLabel.setText("Cart is empty."); return; }
        List<CartItem> items = customer.getCart().getItems();
        javafx.collections.ObservableList<String> displayItems = FXCollections.observableArrayList();
        for (CartItem ci : items) displayItems.add(String.format("%s  |  qty: %d  |  $%.2f", ci.getProductName(), ci.getQuantity(), ci.getSubtotal()));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Cart"); dialog.setHeaderText("Select an item to edit or remove");
        ListView<String> listView = new ListView<>(displayItems); listView.setPrefHeight(160);
        TextField newQtyField = new TextField(); newQtyField.setPromptText("New quantity");
        Label editErr = new Label(""); editErr.setStyle("-fx-text-fill: red;");
        Button updateBtn = new Button("Update Quantity"); Button removeBtn = new Button("Remove Item");
        updateBtn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white;"); removeBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        updateBtn.setOnAction(e -> {
            int idx = listView.getSelectionModel().getSelectedIndex();
            if (idx < 0) { editErr.setText("Select an item first."); return; }
            try {
                int newQty = Integer.parseInt(newQtyField.getText().trim());
                if (newQty <= 0) { editErr.setText("Quantity must be positive."); return; }
                CartItem ci = items.get(idx);
                Stock stockRef = currentProducts.stream().filter(p -> p.getProductId() == ci.getProductId()).findFirst().orElse(null);
                int stockQty = stockRef != null ? stockRef.getQuantity() : Integer.MAX_VALUE;
                if (newQty > stockQty) { editErr.setText("Only " + stockQty + " units in stock."); return; }
                ci.setQuantity(newQty); customer.getCart().calculateTotal(); updateCartView(); dialog.close(); statusLabel.setText("Cart updated.");
            } catch (NumberFormatException ex) { editErr.setText("Enter a valid number."); }
        });
        removeBtn.setOnAction(e -> {
            int idx = listView.getSelectionModel().getSelectedIndex();
            if (idx < 0) { editErr.setText("Select an item first."); return; }
            customer.getCart().removeItem(items.get(idx).getProductId()); updateCartView(); dialog.close(); statusLabel.setText("Item removed from cart.");
        });
        VBox content = new VBox(10, listView, new HBox(8, new Label("New Qty:"), newQtyField), new HBox(10, updateBtn, removeBtn), editErr);
        content.setPadding(new Insets(15));
        dialog.getDialogPane().setContent(content); dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE); dialog.showAndWait();
    }

    private void updateCartView() {
        cartListView.getItems().clear();
        if (customer.getCart() != null) {
            for (CartItem item : customer.getCart().getItems()) cartListView.getItems().add(String.format("%-15s | x%d | $%.2f", item.getProductName(), item.getQuantity(), item.getSubtotal()));
            double cartTotal = customer.getCart().calculateTotal();
            if (pointsDiscountAmount > 0) cartTotal = Math.max(0, cartTotal - pointsDiscountAmount);
            totalLabel.setText("Total: $" + String.format("%.2f", cartTotal));
        }
    }

    @FXML private void handleCheckout() {
        if (customer.getCart().getItems().isEmpty()) { statusLabel.setText("Your cart is empty."); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CheckoutPage.fxml"));
            Parent root = loader.load();
            CheckoutController ctrl = loader.getController();
            ctrl.setCheckoutData(customer, pointsDiscountAmount);
            Stage stage = new Stage(); stage.initModality(Modality.APPLICATION_MODAL); stage.setTitle("Finalize Purchase");
            Scene checkoutScene = new Scene(root);
            checkoutScene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
            stage.setScene(checkoutScene); stage.setMaximized(true); stage.showAndWait();
            updateCartView(); pointsLabel.setText("Points: " + customer.getCustomerPoints()); loadProducts();
        } catch (Exception e) { e.printStackTrace(); statusLabel.setText("Error: Could not load checkout screen."); }
    }

    @FXML private void handleViewHistory() {
        OrderDAO.getOrderHistory(customer.getId(),
            orders -> Platform.runLater(() -> {
                Stage histStage = new Stage();
                histStage.initModality(Modality.APPLICATION_MODAL);
                histStage.setTitle("Order History");

                // ── Header ──
                HBox header = new HBox();
                header.setStyle("-fx-background-color: #1e1035; -fx-padding: 20 28;");
                header.setAlignment(Pos.CENTER_LEFT);
                Label title = new Label("Order History");
                title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #e8e8f0;");
                header.getChildren().add(title);

                // ── Table (GridPane for pixel-perfect column alignment) ──
                GridPane table = new GridPane();
                table.setStyle("-fx-background-color: #1a0a2e;");
                // Columns: ORDER#(8%), ITEMS(42%), TOTAL(17%), DATE(17%), STATUS(16%)
                double[] pcts = {8, 42, 17, 17, 16};
                for (double pct : pcts) {
                    ColumnConstraints cc = new ColumnConstraints();
                    cc.setPercentWidth(pct);
                    table.getColumnConstraints().add(cc);
                }

                // Header row
                String[] headers = {"ORDER #", "ITEMS", "TOTAL", "DATE", "STATUS"};
                for (int c = 0; c < headers.length; c++) {
                    Label h = new Label(headers[c]);
                    h.setStyle("-fx-text-fill: #6b6b85; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1.5px;");
                    h.setPadding(new Insets(10, 8, 10, c == 0 ? 28 : 8));
                    h.setMaxWidth(Double.MAX_VALUE);
                    h.setStyle(h.getStyle() + " -fx-background-color: #12121a; -fx-border-color: rgba(255,255,255,0.06); -fx-border-width: 0 0 1 0;");
                    GridPane.setColumnIndex(h, c);
                    GridPane.setRowIndex(h, 0);
                    table.getChildren().add(h);
                }

                if (orders.isEmpty()) {
                    Label empty = new Label("No orders found.");
                    empty.setStyle("-fx-text-fill: #6b6b85; -fx-font-size: 14px; -fx-padding: 40 28;");
                    GridPane.setColumnIndex(empty, 0);
                    GridPane.setRowIndex(empty, 1);
                    GridPane.setColumnSpan(empty, 5);
                    table.getChildren().add(empty);
                } else {
                    int rowIdx = 1;
                    boolean alt = false;
                    for (Order o : orders) {
                        String bg = alt ? "#12121a" : "#1a0a2e";
                        String border = "-fx-border-color: rgba(255,255,255,0.04); -fx-border-width: 0 0 1 0;";

                        Label orderNum = new Label("#" + o.getOrderId());
                        orderNum.setStyle("-fx-text-fill: #e8e8f0; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-color:" + bg + ";" + border);
                        orderNum.setPadding(new Insets(14, 8, 14, 28));
                        orderNum.setMaxWidth(Double.MAX_VALUE);

                        Label items = new Label(o.getItemsSummary().isEmpty() ? "—" : o.getItemsSummary());
                        items.setStyle("-fx-text-fill: #b0b0c8; -fx-font-size: 13px; -fx-background-color:" + bg + ";" + border);
                        items.setWrapText(true);
                        items.setPadding(new Insets(14, 8, 14, 8));
                        items.setMaxWidth(Double.MAX_VALUE);

                        Label total = new Label("$" + String.format("%,.2f", o.getTotalAmount()));
                        total.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color:" + bg + ";" + border);
                        total.setPadding(new Insets(14, 8, 14, 8));
                        total.setMaxWidth(Double.MAX_VALUE);

                        Label date = new Label(o.getOrderDate());
                        date.setStyle("-fx-text-fill: #6b6b85; -fx-font-size: 12px; -fx-background-color:" + bg + ";" + border);
                        date.setPadding(new Insets(14, 8, 14, 8));
                        date.setMaxWidth(Double.MAX_VALUE);

                        String[] badge = getStatusBadge(o.getStatus() != null ? o.getStatus() : "pending");
                        Label statusLbl = new Label(badge[0]);
                        statusLbl.setStyle("-fx-background-color:" + badge[1] + "; -fx-text-fill:#fff; -fx-font-size:11px; -fx-font-weight:bold; -fx-background-radius:20; -fx-padding:4 12;");
                        HBox statusCell = new HBox(statusLbl);
                        statusCell.setAlignment(Pos.CENTER_LEFT);
                        statusCell.setPadding(new Insets(10, 8, 10, 8));
                        statusCell.setStyle("-fx-background-color:" + bg + ";" + border);
                        statusCell.setMaxWidth(Double.MAX_VALUE);

                        GridPane.setColumnIndex(orderNum,  0); GridPane.setRowIndex(orderNum,  rowIdx);
                        GridPane.setColumnIndex(items,     1); GridPane.setRowIndex(items,     rowIdx);
                        GridPane.setColumnIndex(total,     2); GridPane.setRowIndex(total,     rowIdx);
                        GridPane.setColumnIndex(date,      3); GridPane.setRowIndex(date,      rowIdx);
                        GridPane.setColumnIndex(statusCell,4); GridPane.setRowIndex(statusCell,rowIdx);
                        table.getChildren().addAll(orderNum, items, total, date, statusCell);
                        rowIdx++;
                        alt = !alt;
                    }
                }

                ScrollPane scroll = new ScrollPane(table);
                scroll.setFitToWidth(true);
                scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");
                scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

                VBox root = new VBox(header, scroll);
                root.setStyle("-fx-background-color: #1a0a2e;");
                VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);

                Scene scene = new Scene(root, 1200, 700);
                try { scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm()); } catch (Exception ignored) {}
                histStage.setScene(scene);
                histStage.setMaximized(true);
                histStage.show();
            }),
            e -> Platform.runLater(() -> statusLabel.setText("Error: " + e.getMessage())));
    }

    private String[] getStatusBadge(String status) {
        switch (status.toLowerCase()) {
            case "delivered":  return new String[]{"Delivered",  "#2e7d32"};
            case "confirmed":  return new String[]{"Confirmed",  "#1565c0"};
            case "cancelled":  return new String[]{"Cancelled",  "#b71c1c"};
            case "pending":    return new String[]{"Pending",    "#e65100"};
            default:           return new String[]{status,       "#4a4a6a"};
        }
    }

    @FXML private void handleEditProfile() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit My Profile"); dialog.setHeaderText("Update your account details");
        TextField usernameF = new TextField(customer.getUsername()); TextField emailF = new TextField(customer.getEmail());
        PasswordField passF = new PasswordField(); passF.setPromptText("New password (leave blank to keep current)");
        TextField passVisible = new TextField(); passVisible.setVisible(false); passVisible.setManaged(false);
        final boolean[] showPw = {false};
        Button eyeBtn = new Button("👁"); eyeBtn.setStyle("-fx-cursor: hand;");
        eyeBtn.setOnAction(e -> {
            showPw[0] = !showPw[0];
            if (showPw[0]) { passVisible.setText(passF.getText()); passF.setVisible(false); passF.setManaged(false); passVisible.setVisible(true); passVisible.setManaged(true); eyeBtn.setText("🙈"); }
            else { passF.setText(passVisible.getText()); passVisible.setVisible(false); passVisible.setManaged(false); passF.setVisible(true); passF.setManaged(true); eyeBtn.setText("👁"); }
        });
        HBox passBox = new HBox(5, passF, passVisible, eyeBtn);
        TextField phoneF = new TextField(customer.getPhoneNumber() != null ? customer.getPhoneNumber() : "");
        TextField addressF = new TextField(customer.getAddress() != null ? customer.getAddress() : "");
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(15));
        grid.add(new Label("Username:"), 0, 0); grid.add(usernameF, 1, 0);
        grid.add(new Label("Email:"), 0, 1); grid.add(emailF, 1, 1);
        grid.add(new Label("Password:"), 0, 2); grid.add(passBox, 1, 2);
        grid.add(new Label("Phone (01xxxxxxxxx):"), 0, 3); grid.add(phoneF, 1, 3);
        grid.add(new Label("Address:"), 0, 4); grid.add(addressF, 1, 4);
        Label errLabel = new Label(""); errLabel.setStyle("-fx-text-fill: red;"); grid.add(errLabel, 0, 5, 2, 1);
        dialog.getDialogPane().setContent(grid); dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String uname = usernameF.getText().trim(); String email = emailF.getText().trim(); String phone = phoneF.getText().trim();
            if (uname.isEmpty() || email.isEmpty()) { errLabel.setText("Username and email cannot be empty."); ev.consume(); return; }
            if (!LoginController.isUsernameValid(uname)) { errLabel.setText("Username cannot start with a digit."); ev.consume(); return; }
            if (!LoginController.isEmailValid(email)) { errLabel.setText("Email format invalid."); ev.consume(); return; }
            if (!phone.isEmpty() && !LoginController.isPhoneValid(phone)) { errLabel.setText("Phone must start with 01 and be exactly 11 digits."); ev.consume(); return; }
            String pw = showPw[0] ? passVisible.getText() : passF.getText();
            if (!pw.isEmpty() && !LoginController.isPasswordValid(pw)) { errLabel.setText("Password: 8+ chars, 1 uppercase, 1 lowercase, 1 digit, 1 special."); ev.consume(); }
        });
        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                String pw = showPw[0] ? passVisible.getText() : passF.getText();
                String newPass = pw.isEmpty() ? customer.getPassword() : pw;
                UserDAO.updateCustomerProfile(customer.getId(), usernameF.getText().trim(), emailF.getText().trim(), newPass, phoneF.getText().trim(), addressF.getText().trim(),
                    () -> Platform.runLater(() -> { customer.setUsername(usernameF.getText().trim()); customer.setEmail(emailF.getText().trim()); customer.setPhoneNumber(phoneF.getText().trim()); customer.setAddress(addressF.getText().trim()); statusLabel.setText("Profile updated successfully."); updateCartView(); }),
                    e -> Platform.runLater(() -> statusLabel.setText("Update failed: " + e.getMessage())));
            }
        });
    }

    @FXML private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Scene scene = new Scene(loader.load(), 900, 620);
            scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            stage.setMaximized(false); stage.setWidth(900); stage.setHeight(620); stage.setScene(scene); stage.setMaximized(true); stage.centerOnScreen();
        } catch (Exception e) { e.printStackTrace(); statusLabel.setText("Logout error."); }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(content); a.showAndWait();
    }
}
