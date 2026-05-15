package org.musicStore.gui;

import org.musicStore.dao.StockDAO;
import org.musicStore.dao.UserDAO;
import org.musicStore.dao.VendorDAO;
import org.musicStore.model.Manager;
import org.musicStore.model.Stock;
import org.musicStore.model.Vendor;
import org.musicStore.util.ProductImageUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Modality;
import java.util.List;
import java.util.Map;

public class ManagerDashboard {

    @FXML private FlowPane productGrid;
    @FXML private ScrollPane productScrollPane;
    @FXML private TextField  nameField, brandField, priceField, qtyField, categoryField;
    @FXML private TextField  filterBrand, filterCategory, filterMinPrice, filterMaxPrice, searchField;
    @FXML private Label      statusLabel, managerLabel;
    @FXML private Button     addBtn;
    @FXML private TextArea   reportArea;

    private Manager manager;
    private List<Stock> masterData = FXCollections.observableArrayList();
    private Stock selectedProduct = null;

    @FXML public void initialize() {
        if (productGrid != null && productScrollPane != null) {
            productGrid.prefWidthProperty().bind(productScrollPane.widthProperty().subtract(20));
        }
    }

    public void setManager(Manager m) {
        this.manager = m;
        if (managerLabel != null) managerLabel.setText("Manager: " + m.getUsername());
        loadInventory();
    }

    private void loadInventory() {
        StockDAO.getAllProductsIncludingEmpty(
            products -> Platform.runLater(() -> { masterData = products; renderProductCards(products); }),
            e -> Platform.runLater(() -> statusLabel.setText("DB Error: " + e.getMessage())));
    }

    private void renderProductCards(List<Stock> products) {
        productGrid.getChildren().clear();
        selectedProduct = null;
        if (addBtn != null) addBtn.setText("Add Product");
        for (Stock s : products) productGrid.getChildren().add(createProductCard(s));
    }

    private VBox createProductCard(Stock s) {
        VBox card = new VBox(8);
        card.setPrefWidth(195);
        card.setPrefHeight(255);
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

        int vid = s.getVendorId();
        String vendorName = vid > 0 ? UserDAO.getVendorName(vid) : "Music Store";
        Label vendorLbl = new Label("📦 " + vendorName);
        vendorLbl.setStyle("-fx-font-size: 10px;-fx-text-fill: #7c5cbf;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label priceLbl = new Label(String.format("$%,.0f", s.getPrice()));
        priceLbl.setStyle("-fx-font-size: 17px;-fx-font-weight: bold;-fx-text-fill: #c9a84c;");

        card.getChildren().addAll(badgeRow, iconPane, nameLbl, subLbl, vendorLbl, spacer, priceLbl);

        card.setOnMouseClicked(e -> {
            productGrid.getChildren().forEach(node -> { if (node instanceof VBox) ((VBox) node).setStyle("-fx-background-color: #1e1035;-fx-border-color: rgba(255,255,255,0.08);-fx-border-radius: 14;-fx-background-radius: 14;-fx-border-width: 1;-fx-cursor: hand;"); });
            card.setStyle("-fx-background-color: #2a1a4a;-fx-border-color: #c9a84c;-fx-border-radius: 14;-fx-background-radius: 14;-fx-border-width: 2;-fx-cursor: hand;");
            selectedProduct = s;
            if (nameField != null) nameField.setText(s.getName());
            if (brandField != null) brandField.setText(s.getBrand());
            if (priceField != null) priceField.setText(String.valueOf(s.getPrice()));
            if (qtyField != null) qtyField.setText(String.valueOf(s.getQuantity()));
            if (categoryField != null) categoryField.setText(s.getCategory());
            if (addBtn != null) addBtn.setText("Update Product");
            statusLabel.setText("Editing: " + s.getName());
        });
        card.setOnMouseEntered(e -> { if (selectedProduct != s) card.setStyle("-fx-background-color: #251540;-fx-border-color: rgba(201,168,76,0.4);-fx-border-radius: 14;-fx-background-radius: 14;-fx-border-width: 1;-fx-cursor: hand;"); });
        card.setOnMouseExited(e -> { if (selectedProduct != s) card.setStyle("-fx-background-color: #1e1035;-fx-border-color: rgba(255,255,255,0.08);-fx-border-radius: 14;-fx-background-radius: 14;-fx-border-width: 1;-fx-cursor: hand;"); });
        return card;
    }

    @FXML private void handleSearch() {
        String kw = searchField != null ? searchField.getText().toLowerCase().trim() : "";
        if (kw.isEmpty()) { handleFilter(); return; }
        List<Stock> filtered = masterData.stream().filter(s ->
            s.getName().toLowerCase().contains(kw) ||
            s.getBrand().toLowerCase().contains(kw) ||
            s.getCategory().toLowerCase().contains(kw)
        ).collect(java.util.stream.Collectors.toList());
        renderProductCards(filtered);
        statusLabel.setText("Search: " + filtered.size() + " result(s) for \"" + kw + "\".");
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

    @FXML private void handleAddProduct() {
        try {
            String name = nameField.getText().trim(); String brand = brandField.getText().trim();
            double price = Double.parseDouble(priceField.getText().trim());
            int qty = Integer.parseInt(qtyField.getText().trim());
            String category = categoryField.getText().trim();
            if (name.isEmpty() || brand.isEmpty() || price < 0 || qty < 0) { statusLabel.setText("Error: Invalid input."); return; }
            if (selectedProduct != null) {
                selectedProduct.setName(name); selectedProduct.setBrand(brand);
                selectedProduct.setPrice(price); selectedProduct.setQuantity(qty); selectedProduct.setCategory(category);
                StockDAO.updateProduct(selectedProduct,
                    () -> Platform.runLater(() -> { statusLabel.setText("Updated!"); loadInventory(); handleCancel(); }),
                    e -> Platform.runLater(() -> statusLabel.setText("Update Failed!")));
            } else {
                StockDAO.addProduct(new Stock(name, brand, price, qty, category),
                    () -> Platform.runLater(() -> { statusLabel.setText("Added!"); loadInventory(); handleCancel(); }),
                    e -> Platform.runLater(() -> statusLabel.setText("Add Failed!")));
            }
        } catch (NumberFormatException e) { statusLabel.setText("Error: Check Price/Qty format."); }
    }

    @FXML private void handleDeleteProduct() {
        if (selectedProduct != null) {
            StockDAO.deleteProduct(selectedProduct.getProductId(),
                () -> Platform.runLater(() -> { statusLabel.setText("Deleted."); loadInventory(); handleCancel(); }),
                e -> {});
        } else { statusLabel.setText("Click a product card first."); }
    }

    @FXML private void handleCancel() {
        if (nameField != null) nameField.clear();
        if (brandField != null) brandField.clear();
        if (priceField != null) priceField.clear();
        if (qtyField != null) qtyField.clear();
        if (categoryField != null) categoryField.clear();
        if (addBtn != null) addBtn.setText("Add Product");
        statusLabel.setText("Ready");
        selectedProduct = null;
        productGrid.getChildren().forEach(node -> { if (node instanceof VBox) ((VBox) node).setStyle("-fx-background-color: #1e1035;-fx-border-color: rgba(255,255,255,0.08);-fx-border-radius: 14;-fx-background-radius: 14;-fx-border-width: 1;-fx-cursor: hand;"); });
    }

    @FXML private void handleGenerateReport() {
        VendorDAO.getAllVendorsSalesReport(
            rows -> Platform.runLater(() -> {
                Stage reportStage = new Stage();
                reportStage.initModality(Modality.NONE);
                reportStage.setTitle("Sales Report — ASH Store");
                // ── Header ──
                HBox header = new HBox();
                header.setStyle("-fx-background-color: #1e1035; -fx-padding: 20 28;");
                header.setAlignment(Pos.CENTER_LEFT);
                Label title = new Label("Full Sales Report — All Vendors");
                title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #e8e8f0;");
                Label subtitle = new Label("Gross Rev = 70% of price  |  Vendor share = 70% of Gross  |  Store share = 30% of Gross");
                subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #9b86d0; -fx-padding: 4 0 0 20;");
                header.getChildren().addAll(title, subtitle);

                // Columns: NAME(18%), BRAND(10%), CATEGORY(10%), PRICE(7%), SOLD(6%), TOTAL(12%), GROSS(12%), STORE(13%), VENDOR(12%)
                GridPane table = new GridPane();
                table.setStyle("-fx-background-color: #1a0a2e;");
                double[] pcts = {18, 10, 10, 7, 6, 12, 12, 13, 12};
                for (double pct : pcts) {
                    ColumnConstraints cc = new ColumnConstraints();
                    cc.setPercentWidth(pct);
                    table.getColumnConstraints().add(cc);
                }
                String[] colHeaders = {"PRODUCT NAME", "BRAND", "CATEGORY", "PRICE", "SOLD", "TOTAL PRICE", "GROSS REV", "STORE SHARE", "VENDOR SHARE"};
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

                double totalPrice = 0, totalGross = 0, totalStore = 0, totalVendor = 0;
                if (rows.isEmpty()) {
                    Label empty = new Label("No sold items yet.");
                    empty.setStyle("-fx-text-fill: #6b6b85; -fx-font-size: 14px; -fx-padding: 40 28;");
                    GridPane.setColumnIndex(empty, 0); GridPane.setRowIndex(empty, 1);
                    GridPane.setColumnSpan(empty, 9);
                    table.getChildren().add(empty);
                } else {
                    int rowIdx = 1; boolean alt = false;
                    for (String[] r : rows) {
                        // r: [name, brand, category, price, totalSold, totalPrice, grossRev, storeShare]
                        double tp = 0, gross = 0, store = 0;
                        try { tp    = Double.parseDouble(r[5].replace("$","")); } catch(Exception ignored){}
                        try { gross = Double.parseDouble(r[6].replace("$","")); } catch(Exception ignored){}
                        try { store = Double.parseDouble(r[7].replace("$","").replace(" (store)","").trim()); } catch(Exception ignored){}
                        double vendorShare = gross - store;
                        totalPrice += tp; totalGross += gross; totalStore += store; totalVendor += vendorShare;

                        String bg = alt ? "#12121a" : "#1a0a2e";
                        String border = "-fx-border-color: rgba(255,255,255,0.04); -fx-border-width: 0 0 1 0;";
                        String[] vals = {r[0], r[1], r[2], r[3], r[4],
                            "$" + String.format("%,.2f", tp),
                            "$" + String.format("%,.2f", gross),
                            "$" + String.format("%,.2f", store),
                            "$" + String.format("%,.2f", vendorShare)};
                        for (int c = 0; c < vals.length; c++) {
                            Label cell = new Label(vals[c]);
                            String color;
                            if (c == 0) color = "#e8e8f0";
                            else if (c == 5) color = "#c9a84c";
                            else if (c == 6) color = "#9b86d0";
                            else if (c == 7) color = "#3ddc84";
                            else if (c == 8) color = "#5bc8f5";
                            else color = "#b0b0c8";
                            cell.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px;" +
                                "-fx-background-color: " + bg + "; " + border);
                            cell.setPadding(new Insets(14, 8, 14, c == 0 ? 28 : 8));
                            cell.setMaxWidth(Double.MAX_VALUE);
                            cell.setWrapText(true);
                            GridPane.setColumnIndex(cell, c);
                            GridPane.setRowIndex(cell, rowIdx);
                            table.getChildren().add(cell);
                        }
                        alt = !alt; rowIdx++;
                    }
                }

                // ── Footer totals ──
                HBox footer = new HBox(30);
                footer.setStyle("-fx-background-color: #1e1035; -fx-padding: 16 28; -fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 1 0 0 0;");
                footer.setAlignment(Pos.CENTER_LEFT);
                Label l1 = new Label("Total Price: $" + String.format("%,.2f", totalPrice));
                l1.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #c9a84c;");
                Label l2 = new Label("Gross Rev: $" + String.format("%,.2f", totalGross));
                l2.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #9b86d0;");
                Label l3 = new Label("Store Share: $" + String.format("%,.2f", totalStore));
                l3.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3ddc84;");
                Label l4 = new Label("Vendor Share: $" + String.format("%,.2f", totalVendor));
                l4.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #5bc8f5;");
                footer.getChildren().addAll(l1, l2, l3, l4);

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
        VendorDAO.getDailyProfitAllVendors(
            data -> Platform.runLater(() -> {
                if (data.isEmpty()) { showAlert("No Data", "No sales data available yet for a graph.", Alert.AlertType.INFORMATION); return; }
                CategoryAxis xAxis = new CategoryAxis(); NumberAxis yAxis = new NumberAxis();
                xAxis.setLabel("Date"); yAxis.setLabel("Store Profit ($)");
                LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
                chart.setTitle("Daily Store Profit (Own products: 70% of price | Vendor products: 21% of price)");
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

    @FXML private void handleViewPendingVendors() {
        VendorDAO.getPendingVendors(vendors -> Platform.runLater(() -> {
            if (vendors.isEmpty()) { showAlert("Pending Vendors", "No pending vendors.", Alert.AlertType.INFORMATION); return; }
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Pending Vendor Approvals"); dialog.setHeaderText("Select a vendor and approve or reject:");
            ListView<String> listView = new ListView<>();
            for (Vendor v : vendors) listView.getItems().add("ID:" + v.getId() + " | " + v.getUsername() + " (" + v.getVendorName() + ") — " + v.getEmail());
            listView.setPrefHeight(180);
            Button approveBtn = new Button("✅ Approve"); Button rejectBtn = new Button("❌ Reject");
            approveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
            rejectBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
            approveBtn.setOnAction(e -> {
                int idx = listView.getSelectionModel().getSelectedIndex(); if (idx < 0) return;
                Vendor sel = vendors.get(idx);
                VendorDAO.approveVendor(sel.getId(), () -> Platform.runLater(() -> { statusLabel.setText("Approved: " + sel.getUsername()); listView.getItems().remove(idx); vendors.remove(idx); }), err -> {});
            });
            rejectBtn.setOnAction(e -> {
                int idx = listView.getSelectionModel().getSelectedIndex(); if (idx < 0) return;
                Vendor sel = vendors.get(idx);
                VendorDAO.rejectVendor(sel.getId(), () -> Platform.runLater(() -> { statusLabel.setText("Rejected: " + sel.getUsername()); listView.getItems().remove(idx); vendors.remove(idx); }), err -> {});
            });
            VBox content = new VBox(10, listView, new HBox(10, approveBtn, rejectBtn));
            content.setPadding(new Insets(15));
            dialog.getDialogPane().setContent(content); dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE); dialog.showAndWait();
        }), e -> Platform.runLater(() -> statusLabel.setText("Error: " + e.getMessage())));
    }

    @FXML private void handleRemoveVendor() {
        VendorDAO.getApprovedVendors(vendors -> Platform.runLater(() -> {
            if (vendors.isEmpty()) { showAlert("Remove Vendor", "No approved vendors found.", Alert.AlertType.INFORMATION); return; }
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Remove Vendor"); dialog.setHeaderText("Select a vendor to remove:");
            ListView<String> listView = new ListView<>();
            javafx.collections.ObservableList<String> allItems = javafx.collections.FXCollections.observableArrayList();
            for (Vendor v : vendors) allItems.add("ID:" + v.getId() + " | " + v.getUsername() + " (" + v.getVendorName() + ") — " + v.getEmail());
            listView.setItems(allItems);
            listView.setPrefHeight(200);

            // Search field
            TextField searchVendor = new TextField();
            searchVendor.setPromptText("🔍 Search vendors...");
            searchVendor.setStyle("-fx-font-size: 13px;");
            searchVendor.textProperty().addListener((obs, old, val) -> {
                String q = val.toLowerCase().trim();
                if (q.isEmpty()) { listView.setItems(allItems); }
                else {
                    javafx.collections.ObservableList<String> filtered = javafx.collections.FXCollections.observableArrayList();
                    for (String s : allItems) if (s.toLowerCase().contains(q)) filtered.add(s);
                    listView.setItems(filtered);
                }
            });

            Label warn = new Label("⚠ This will permanently remove the vendor account.");
            warn.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 12px;");
            Button removeBtn = new Button("🗑 Remove Selected Vendor");
            removeBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
            removeBtn.setOnAction(e -> {
                String selected = listView.getSelectionModel().getSelectedItem(); if (selected == null) return;
                int idx = allItems.indexOf(selected); if (idx < 0) return;
                Vendor sel = vendors.get(idx);
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm Remove"); confirm.setHeaderText(null);
                confirm.setContentText("Remove vendor '" + sel.getUsername() + " (" + sel.getVendorName() + ")'?\nTheir products will remain in inventory.");
                confirm.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.OK) {
                        VendorDAO.rejectVendor(sel.getId(),
                            () -> Platform.runLater(() -> { statusLabel.setText("Vendor removed: " + sel.getUsername()); allItems.remove(idx); vendors.remove(idx); }),
                            err -> Platform.runLater(() -> statusLabel.setText("Failed to remove vendor.")));
                    }
                });
            });
            VBox content = new VBox(10, searchVendor, listView, warn, removeBtn); content.setPadding(new Insets(15));
            dialog.getDialogPane().setContent(content); dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE); dialog.showAndWait();
        }), e -> Platform.runLater(() -> statusLabel.setText("Error: " + e.getMessage())));
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
                UserDAO.updatePassword(manager.getId(), pw,
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
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(content); a.showAndWait();
    }
}
