package org.musicStore.gui;

import org.musicStore.dao.StockDAO;
import org.musicStore.dao.UserDAO;
import org.musicStore.dao.VendorDAO;
import org.musicStore.model.Stock;
import org.musicStore.model.Vendor;
import org.musicStore.util.ProductImageUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Modality;
import javafx.application.Platform;
import javafx.stage.Stage;
import java.util.List;
import java.util.Map;

public class VendorDashboard {

    @FXML private FlowPane productGrid;
    @FXML private ScrollPane productScrollPane;
    @FXML private TextField searchField;
    @FXML private TextField filterBrand, filterCategory, filterMinPrice, filterMaxPrice;
    @FXML private TextField nameField, brandField, priceField, qtyField, categoryField;
    @FXML private Label statusLabel, vendorLabel;
    @FXML private Button addBtn;
    @FXML private TextArea reportArea;

    private Vendor vendor;
    private List<Stock> masterData = FXCollections.observableArrayList();
    private Stock selectedProduct = null;

    @FXML
    public void initialize() {
        if (productGrid != null && productScrollPane != null) {
            productGrid.prefWidthProperty().bind(productScrollPane.widthProperty().subtract(20));
        }
    }

    public void setVendor(Vendor v) {
        this.vendor = v;
        vendorLabel.setText("Vendor: " + v.getUsername() + " | " + v.getVendorName());
        loadMyItems();
    }

    private void loadMyItems() {
        StockDAO.getProductsByVendor(vendor.getId(),
            products -> Platform.runLater(() -> { masterData = products; renderProductCards(products); }),
            e -> Platform.runLater(() -> statusLabel.setText("Error: " + e.getMessage())));
    }

    private void renderProductCards(List<Stock> products) {
        productGrid.getChildren().clear();
        selectedProduct = null;
        if (addBtn != null) addBtn.setText("Add Item");
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

        Label qtyLbl = new Label("Qty: " + s.getQuantity());
        qtyLbl.setStyle("-fx-font-size: 11px;-fx-text-fill: #8b8ba0;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label priceLbl = new Label(String.format("$%,.0f", s.getPrice()));
        priceLbl.setStyle("-fx-font-size: 17px;-fx-font-weight: bold;-fx-text-fill: #c9a84c;");

        card.getChildren().addAll(badgeRow, iconPane, nameLbl, subLbl, qtyLbl, spacer, priceLbl);

        card.setOnMouseClicked(e -> {
            productGrid.getChildren().forEach(node -> { if (node instanceof VBox) ((VBox) node).setStyle("-fx-background-color: #1e1035;-fx-border-color: rgba(255,255,255,0.08);-fx-border-radius: 14;-fx-background-radius: 14;-fx-border-width: 1;-fx-cursor: hand;"); });
            card.setStyle("-fx-background-color: #2a1a4a;-fx-border-color: #c9a84c;-fx-border-radius: 14;-fx-background-radius: 14;-fx-border-width: 2;-fx-cursor: hand;");
            selectedProduct = s;
            // Populate form fields
            if (nameField != null) nameField.setText(s.getName());
            if (brandField != null) brandField.setText(s.getBrand());
            if (priceField != null) priceField.setText(String.valueOf(s.getPrice()));
            if (qtyField != null) qtyField.setText(String.valueOf(s.getQuantity()));
            if (categoryField != null) categoryField.setText(s.getCategory());
            if (addBtn != null) addBtn.setText("Update Item");
            statusLabel.setText("Selected: " + s.getName());
        });
        card.setOnMouseEntered(e -> { if (selectedProduct != s) card.setStyle("-fx-background-color: #251540;-fx-border-color: rgba(201,168,76,0.4);-fx-border-radius: 14;-fx-background-radius: 14;-fx-border-width: 1;-fx-cursor: hand;"); });
        card.setOnMouseExited(e -> { if (selectedProduct != s) card.setStyle("-fx-background-color: #1e1035;-fx-border-color: rgba(255,255,255,0.08);-fx-border-radius: 14;-fx-background-radius: 14;-fx-border-width: 1;-fx-cursor: hand;"); });
        return card;
    }

    @FXML private void handleFilter() {
        String b  = filterBrand    != null ? filterBrand.getText().toLowerCase()    : "";
        String c  = filterCategory != null ? filterCategory.getText().toLowerCase() : "";
        String mn = filterMinPrice != null ? filterMinPrice.getText().trim()         : "";
        String mx = filterMaxPrice != null ? filterMaxPrice.getText().trim()         : "";
        List<Stock> filtered = masterData.stream().filter(s -> {
            if (!b.isEmpty()  && !s.getBrand().toLowerCase().contains(b))    return false;
            if (!c.isEmpty()  && !s.getCategory().toLowerCase().contains(c)) return false;
            if (!mn.isEmpty()) { try { if (s.getPrice() < Double.parseDouble(mn)) return false; } catch (NumberFormatException ignored) {} }
            if (!mx.isEmpty()) { try { if (s.getPrice() > Double.parseDouble(mx)) return false; } catch (NumberFormatException ignored) {} }
            return true;
        }).collect(java.util.stream.Collectors.toList());
        renderProductCards(filtered);
        statusLabel.setText("Filtered: " + filtered.size() + " products.");
    }

    @FXML private void handleSearch() {
        String keyword = searchField != null ? searchField.getText().trim() : "";
        if (keyword.isEmpty()) { loadMyItems(); return; }
        StockDAO.searchByNameForVendor(vendor.getId(), keyword,
            results -> Platform.runLater(() -> { masterData = results; renderProductCards(results); statusLabel.setText("Search: " + results.size() + " result(s)."); }),
            e -> Platform.runLater(() -> statusLabel.setText("Search error: " + e.getMessage())));
    }

    @FXML private void handleClearSearch() { if (searchField != null) searchField.clear(); loadMyItems(); }

    @FXML private void handleAddProduct() {
        try {
            String name = nameField.getText().trim();
            String brand = brandField.getText().trim();
            double price = Double.parseDouble(priceField.getText().trim());
            int qty = Integer.parseInt(qtyField.getText().trim());
            String category = categoryField.getText().trim();
            if (name.isEmpty() || brand.isEmpty() || category.isEmpty()) { statusLabel.setText("Please fill all fields."); return; }

            if (selectedProduct != null) {
                selectedProduct.setName(name); selectedProduct.setBrand(brand);
                selectedProduct.setPrice(price); selectedProduct.setQuantity(qty); selectedProduct.setCategory(category);
                StockDAO.updateProduct(selectedProduct,
                    () -> Platform.runLater(() -> { statusLabel.setText("Item updated."); loadMyItems(); handleCancel(); }),
                    e -> Platform.runLater(() -> statusLabel.setText("Update failed: " + e.getMessage())));
            } else {
                StockDAO.addProductForVendor(new Stock(name, brand, price, qty, category), vendor.getId(),
                    () -> Platform.runLater(() -> { statusLabel.setText("Item added!"); loadMyItems(); handleCancel(); }),
                    e -> Platform.runLater(() -> statusLabel.setText("Add failed: " + e.getMessage())));
            }
        } catch (NumberFormatException e) { statusLabel.setText("Invalid price or quantity format."); }
    }

    @FXML private void handleDeleteProduct() {
        if (selectedProduct != null) {
            if (selectedProduct.getVendorId() != vendor.getId()) { statusLabel.setText("You can only delete your own products."); return; }
            StockDAO.deleteProduct(selectedProduct.getProductId(),
                () -> Platform.runLater(() -> { statusLabel.setText("Deleted."); loadMyItems(); handleCancel(); }),
                e -> {});
        } else { statusLabel.setText("Click a product card first."); }
    }

    @FXML private void handleCancel() {
        if (nameField != null) nameField.clear();
        if (brandField != null) brandField.clear();
        if (priceField != null) priceField.clear();
        if (qtyField != null) qtyField.clear();
        if (categoryField != null) categoryField.clear();
        if (addBtn != null) addBtn.setText("Add Item");
        statusLabel.setText("Ready");
        selectedProduct = null;
        productGrid.getChildren().forEach(node -> { if (node instanceof VBox) ((VBox) node).setStyle("-fx-background-color: #1e1035;-fx-border-color: rgba(255,255,255,0.08);-fx-border-radius: 14;-fx-background-radius: 14;-fx-border-width: 1;-fx-cursor: hand;"); });
    }

    @FXML private void handleGenerateReport() {
        VendorDAO.getVendorSalesReport(vendor.getId(),
            rows -> Platform.runLater(() -> {
                Stage reportStage = new Stage();
                reportStage.initModality(Modality.NONE);
                reportStage.setTitle("Sales Report — " + vendor.getVendorName());
                // ── Header ──
                HBox header = new HBox();
                header.setStyle("-fx-background-color: #1e1035; -fx-padding: 20 28;");
                header.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(header, Priority.ALWAYS);
                Label title = new Label("Sales Report — " + vendor.getVendorName());
                title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #e8e8f0;");
                Label subtitle = new Label("Your Revenue = 70% of Gross Revenue (49% of sale price)");
                subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #9b86d0; -fx-padding: 4 0 0 20;");
                header.getChildren().addAll(title, subtitle);

                // ── Table columns: NAME(22%), BRAND(12%), CATEGORY(12%), PRICE(9%), SOLD(8%), TOTAL(15%), REVENUE(22%) ──
                GridPane table = new GridPane();
                table.setStyle("-fx-background-color: #1a0a2e;");
                double[] pcts = {22, 12, 12, 9, 8, 15, 22};
                for (double pct : pcts) {
                    ColumnConstraints cc = new ColumnConstraints();
                    cc.setPercentWidth(pct);
                    table.getColumnConstraints().add(cc);
                }
                String[] colHeaders = {"PRODUCT NAME", "BRAND", "CATEGORY", "PRICE", "SOLD QTY", "TOTAL PRICE", "YOUR REVENUE (70%)"};
                for (int c = 0; c < colHeaders.length; c++) {
                    Label h = new Label(colHeaders[c]);
                    h.setStyle("-fx-text-fill: #6b6b85; -fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-background-color: #12121a; -fx-border-color: rgba(255,255,255,0.06);" +
                        "-fx-border-width: 0 0 1 0;");
                    h.setPadding(new Insets(10, 8, 10, c == 0 ? 28 : 8));
                    h.setMaxWidth(Double.MAX_VALUE);
                    GridPane.setColumnIndex(h, c);
                    GridPane.setRowIndex(h, 0);
                    table.getChildren().add(h);
                }

                double totalRevenue = 0, totalPrice = 0;
                if (rows.isEmpty()) {
                    Label empty = new Label("No sold items yet.");
                    empty.setStyle("-fx-text-fill: #6b6b85; -fx-font-size: 14px; -fx-padding: 40 28;");
                    GridPane.setColumnIndex(empty, 0); GridPane.setRowIndex(empty, 1);
                    GridPane.setColumnSpan(empty, 7);
                    table.getChildren().add(empty);
                } else {
                    int rowIdx = 1; boolean alt = false;
                    for (String[] r : rows) {
                        // r: [name, brand, category, price, soldQty, totalPrice, grossRev]
                        String bg = alt ? "#12121a" : "#1a0a2e";
                        String border = "-fx-border-color: rgba(255,255,255,0.04); -fx-border-width: 0 0 1 0;";
                        String[] vals = {r[0], r[1], r[2], r[3], r[4], r[5], r[6]};
                        for (int c = 0; c < vals.length; c++) {
                            Label cell = new Label(vals[c]);
                            String color = c == 5 ? "#c9a84c" : c == 6 ? "#3ddc84" : "#b0b0c8";
                            if (c == 0) color = "#e8e8f0";
                            cell.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px;" +
                                "-fx-background-color: " + bg + "; " + border);
                            cell.setPadding(new Insets(14, 8, 14, c == 0 ? 28 : 8));
                            cell.setMaxWidth(Double.MAX_VALUE);
                            cell.setWrapText(true);
                            GridPane.setColumnIndex(cell, c);
                            GridPane.setRowIndex(cell, rowIdx);
                            table.getChildren().add(cell);
                        }
                        try { totalPrice   += Double.parseDouble(r[5].replace("$", "")); } catch (Exception ignored) {}
                        try { totalRevenue += Double.parseDouble(r[6].replaceAll("\\$([0-9.]+).*", "$1")); } catch (Exception ignored) {}
                        alt = !alt; rowIdx++;
                    }
                }

                // ── Footer totals ──
                HBox footer = new HBox(40);
                footer.setStyle("-fx-background-color: #1e1035; -fx-padding: 16 28; -fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 1 0 0 0;");
                footer.setAlignment(Pos.CENTER_LEFT);
                Label totPriceLbl = new Label("Total Price: $" + String.format("%,.2f", totalPrice));
                totPriceLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #c9a84c;");
                Label totRevLbl  = new Label("Your Revenue (70% of Gross): $" + String.format("%,.2f", totalRevenue));
                totRevLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3ddc84;");
                footer.getChildren().addAll(totPriceLbl, totRevLbl);

                ScrollPane scroll = new ScrollPane(table);
                scroll.setFitToWidth(true);
                scroll.setStyle("-fx-background-color: transparent;");
                VBox root = new VBox(header, scroll, footer);
                VBox.setVgrow(scroll, Priority.ALWAYS);
                root.setStyle("-fx-background-color: #1a0a2e;");

                javafx.scene.Scene scene = new javafx.scene.Scene(root);
                scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
                reportStage.setScene(scene);
                reportStage.show();
                reportStage.setMaximized(true);
                if (reportArea != null) reportArea.setText("Report opened in new window.");
            }),
            e -> Platform.runLater(() -> { if (reportArea != null) reportArea.setText("Report error: " + e.getMessage()); }));
    }

    @FXML private void handleShowProfitGraph() {
        VendorDAO.getDailyProfitForVendor(vendor.getId(),
            data -> Platform.runLater(() -> {
                if (data.isEmpty()) { showAlert("No Data", "No sales data yet to display a graph.", Alert.AlertType.INFORMATION); return; }
                CategoryAxis xAxis = new CategoryAxis(); NumberAxis yAxis = new NumberAxis();
                xAxis.setLabel("Date"); yAxis.setLabel("Your Profit ($) — 70% of Gross Rev");
                LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
                chart.setTitle("Daily Profit — " + vendor.getVendorName() + " (70% of Gross Revenue)");
                chart.setCreateSymbols(true); chart.setAnimated(false);
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Daily Profit");
                for (Map.Entry<String, Double> entry : data.entrySet()) series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                chart.getData().add(series);
                chart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                javafx.scene.layout.VBox.setVgrow(chart, javafx.scene.layout.Priority.ALWAYS);
                Stage graphStage = new Stage(); graphStage.initModality(Modality.APPLICATION_MODAL);
                graphStage.setTitle("Daily Profit Graph");
                VBox graphRoot = new VBox(chart);
                graphRoot.setStyle("-fx-background-color: #1a0a2e;");
                graphRoot.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                javafx.scene.layout.VBox.setVgrow(chart, javafx.scene.layout.Priority.ALWAYS);
                javafx.scene.Scene graphScene = new javafx.scene.Scene(graphRoot);
                graphScene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
                graphStage.setScene(graphScene);
                graphStage.show();
                graphStage.setMaximized(true);
            }),
            e -> Platform.runLater(() -> statusLabel.setText("Graph error: " + e.getMessage())));
    }

    @FXML private void handleChangePassword() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Change Password"); dlg.setHeaderText("Enter your new password");
        PasswordField newPass = new PasswordField(); newPass.setPromptText("New password");
        TextField newPassVis = new TextField(); newPassVis.setVisible(false); newPassVis.setManaged(false);
        PasswordField confirm = new PasswordField(); confirm.setPromptText("Confirm password");
        TextField confirmVis = new TextField(); confirmVis.setVisible(false); confirmVis.setManaged(false);
        final boolean[] show = {false};
        Button eye = new Button("👁"); eye.setStyle("-fx-cursor: hand;");
        eye.setOnAction(e -> {
            show[0] = !show[0];
            if (show[0]) {
                newPassVis.setText(newPass.getText()); confirmVis.setText(confirm.getText());
                newPass.setVisible(false); newPass.setManaged(false); newPassVis.setVisible(true); newPassVis.setManaged(true);
                confirm.setVisible(false); confirm.setManaged(false); confirmVis.setVisible(true); confirmVis.setManaged(true); eye.setText("🙈");
            } else {
                newPass.setText(newPassVis.getText()); confirm.setText(confirmVis.getText());
                newPassVis.setVisible(false); newPassVis.setManaged(false); newPass.setVisible(true); newPass.setManaged(true);
                confirmVis.setVisible(false); confirmVis.setManaged(false); confirm.setVisible(true); confirm.setManaged(true); eye.setText("👁");
            }
        });
        Label err = new Label(""); err.setStyle("-fx-text-fill: red;");
        Label hint = new Label("Min 8 chars, uppercase, lowercase, digit & special character"); hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        VBox box = new VBox(8, new Label("New Password:"), new HBox(5, newPass, newPassVis, eye), new Label("Confirm:"), confirm, confirmVis, hint, err);
        box.setPadding(new Insets(15));
        dlg.getDialogPane().setContent(box); dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button okBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String pw = show[0] ? newPassVis.getText() : newPass.getText();
            String pw2 = show[0] ? confirmVis.getText() : confirm.getText();
            if (!pw.equals(pw2)) { err.setText("Passwords do not match."); ev.consume(); }
            else if (!LoginController.isPasswordValid(pw)) { err.setText("Password: 8+ chars, 1 uppercase, 1 lowercase, 1 digit, 1 special."); ev.consume(); }
        });
        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                String pw = show[0] ? newPassVis.getText() : newPass.getText();
                UserDAO.updatePassword(vendor.getId(), pw,
                    () -> Platform.runLater(() -> statusLabel.setText("Password updated.")),
                    e -> Platform.runLater(() -> statusLabel.setText("Failed: " + e.getMessage())));
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
        } catch (Exception e) { e.printStackTrace(); statusLabel.setText("Logout error: " + e.getMessage()); }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(content); a.showAndWait();
    }
}
