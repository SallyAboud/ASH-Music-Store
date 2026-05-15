package org.musicStore.gui;

import org.musicStore.dao.*;
import org.musicStore.model.*;
import org.musicStore.util.EmailUtil;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.application.Platform;
import javafx.geometry.Insets;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField     passwordVisible;
    @FXML private Button        togglePasswordBtn;
    @FXML private Label         errorLabel;
    @FXML private Button        loginButton;
    @FXML private Button        registerButton;
    @FXML private Button        forgotPasswordButton;

    private boolean showingPassword = false;

    // ── Validators (static so CheckoutController / CustomerDashboard can use them) ──
    public static boolean isPasswordValid(String password) {
        if (password.length() < 8) return false;
        boolean hasUpper   = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower   = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit   = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(c -> "!@#$%^&*()_+-=[]{}|;':\",./<>?".indexOf(c) >= 0);
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
    public static boolean isEmailValid(String email) {
        if (email == null || email.isEmpty() || Character.isDigit(email.charAt(0))) return false;
        // Accept any valid email: letter@domain.tld (any TLD, including .edu.eg, .com, .org, .net, etc.)
        return email.matches("^[A-Za-z][A-Za-z0-9._%-]*@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    // FIX 3: phone must start with 01, 11 digits total
    public static boolean isPhoneValid(String phone) {
        return phone.matches("01\\d{9}");
    }
    public static boolean isUsernameValid(String username) {
        return !username.isEmpty() && !Character.isDigit(username.charAt(0));
    }

    @FXML public void initialize() {
        if (passwordVisible != null) {
            passwordVisible.setVisible(false);
            passwordVisible.setManaged(false);
            passwordField.textProperty().addListener((obs,o,n) -> { if (!showingPassword) passwordVisible.setText(n); });
            passwordVisible.textProperty().addListener((obs,o,n) -> { if (showingPassword) passwordField.setText(n); });
        }
    }

    @FXML private void handleTogglePassword() {
        showingPassword = !showingPassword;
        if (showingPassword) {
            passwordVisible.setText(passwordField.getText());
            passwordField.setVisible(false);  passwordField.setManaged(false);
            passwordVisible.setVisible(true); passwordVisible.setManaged(true);
            if (togglePasswordBtn != null) togglePasswordBtn.setText("🙈");
        } else {
            passwordField.setText(passwordVisible.getText());
            passwordVisible.setVisible(false); passwordVisible.setManaged(false);
            passwordField.setVisible(true);    passwordField.setManaged(true);
            if (togglePasswordBtn != null) togglePasswordBtn.setText("👁");
        }
    }

    @FXML private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = showingPassword ? passwordVisible.getText() : passwordField.getText();
        if (email.isEmpty() || password.isEmpty()) { errorLabel.setText("Please enter email and password."); return; }
        loginButton.setDisable(true);
        errorLabel.setText("Logging in...");
        UserDAO.authenticate(email, password,
            user -> Platform.runLater(() -> { loginButton.setDisable(false); openDashboard(user); }),
            error -> Platform.runLater(() -> { loginButton.setDisable(false); errorLabel.setText("Invalid email or password."); }));
    }

    private void openDashboard(User user) {
        try {
            String fxmlPath;
            if (user instanceof Manager)     fxmlPath = "/fxml/ManagerDashboard.fxml";
            else if (user instanceof Vendor) {
                Vendor vendor = (Vendor) user;
                if (!vendor.isApproved()) { errorLabel.setText("Your vendor account is pending manager approval."); return; }
                fxmlPath = "/fxml/VendorDashboard.fxml";
            } else fxmlPath = "/fxml/CustomerDashboard.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            if (loader.getLocation() == null) { errorLabel.setText("FXML Not Found: " + fxmlPath); return; }
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
            if      (user instanceof Manager)  { ManagerDashboard  ctrl = loader.getController(); ctrl.setManager((Manager) user); }
            else if (user instanceof Vendor)   { VendorDashboard   ctrl = loader.getController(); ctrl.setVendor((Vendor) user);  }
            else                               { CustomerDashboard ctrl = loader.getController(); ctrl.setCustomer((Customer) user); }
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setMaximized(false);
            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Navigation Error: " + e.getMessage());
        }
    }

    // ── FORGOT PASSWORD ───────────────────────────────────────────────────────
    @FXML private void handleForgotPassword() {
        // Step 1 — ask for email
        TextInputDialog emailDialog = new TextInputDialog();
        emailDialog.setTitle("Forgot Password");
        emailDialog.setHeaderText("Reset Your Password");
        emailDialog.setContentText("Enter your registered email address:");
        styleDialog(emailDialog.getDialogPane());
        emailDialog.getEditor().setStyle("-fx-text-fill: black; -fx-background-color: white; -fx-prompt-text-fill: #999;");

        emailDialog.showAndWait().ifPresent(email -> {
            if (email.trim().isEmpty()) { errorLabel.setText("Please enter your email."); return; }
            errorLabel.setText("Sending reset code…");

            UserDAO.sendPasswordResetCode(email.trim(),
                username -> Platform.runLater(() -> {
                    errorLabel.setText("");
                    showOTPAndResetDialog(email.trim(), username);
                }),
                err -> Platform.runLater(() ->
                    errorLabel.setText("⚠ " + err.getMessage())));
        });
    }

    /** Step 2 — verify OTP then let user set a new password. */
    private void showOTPAndResetDialog(String email, String username) {
        String inputStyle = "-fx-text-fill: black; -fx-background-color: white; -fx-prompt-text-fill: #999;";

        Label infoLbl  = new Label("A 6-digit code was sent to: " + email);
        infoLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#aaa;");

        TextField otpField = new TextField();
        otpField.setPromptText("Enter 6-digit code");
        otpField.setStyle(inputStyle);

        PasswordField newPassField  = new PasswordField();
        newPassField.setPromptText("New password");
        newPassField.setStyle(inputStyle);

        PasswordField confirmField  = new PasswordField();
        confirmField.setPromptText("Confirm new password");
        confirmField.setStyle(inputStyle);

        Label hint = new Label("Min 8 chars — uppercase, lowercase, digit & special character");
        hint.setStyle("-fx-font-size:11px;-fx-text-fill:#888;");

        Label errLbl = new Label("");
        errLbl.setStyle("-fx-text-fill:red;-fx-font-size:12px;-fx-wrap-text:true;");
        errLbl.setMaxWidth(400);

        VBox content = new VBox(8, infoLbl,
                new Label("Verification Code:"), otpField,
                new Label("New Password:"), newPassField,
                new Label("Confirm Password:"), confirmField, hint, errLbl);
        content.setPadding(new Insets(15));
        content.setPrefWidth(420);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reset Password");
        dialog.setHeaderText("Enter Code & New Password");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleDialog(dialog.getDialogPane());

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String code    = otpField.getText().trim();
            String newPass = newPassField.getText();
            String confirm = confirmField.getText();

            if (code.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                errLbl.setText("Please fill in all fields."); ev.consume(); return;
            }
            if (!UserDAO.validateOTP(email, code)) {
                errLbl.setText("⚠ Invalid or expired code. Please try again."); ev.consume(); return;
            }
            if (!newPass.equals(confirm)) {
                errLbl.setText("⚠ Passwords do not match."); ev.consume(); return;
            }
            if (!isPasswordValid(newPass)) {
                errLbl.setText("⚠ Password: 8+ chars, uppercase, lowercase, digit, special char."); ev.consume(); return;
            }
        });

        dialog.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                // OTP already validated above — now update password by email lookup
                org.musicStore.util.DBUtil.DB_EXECUTOR.execute(() -> {
                    try (java.sql.Connection conn = org.musicStore.util.DBUtil.getConnection();
                         java.sql.PreparedStatement ps = conn.prepareStatement(
                                 "SELECT id FROM User WHERE email = ?")) {
                        ps.setString(1, email);
                        java.sql.ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            int uid = rs.getInt("id");
                            UserDAO.updatePassword(uid, newPassField.getText(),
                                () -> Platform.runLater(() -> {
                                    errorLabel.setText("✅ Password reset successfully! Please log in.");
                                }),
                                e -> Platform.runLater(() ->
                                    errorLabel.setText("⚠ Failed to update password: " + e.getMessage())));
                        }
                    } catch (Exception ex) {
                        Platform.runLater(() -> errorLabel.setText("⚠ Error: " + ex.getMessage()));
                    }
                });
            }
        });
    }

    // ── REGISTER ─────────────────────────────────────────────────────────────
    @FXML private void handleRegister() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create an Account — ASH Store");
        dialog.setHeaderText("Register for ASH Store");

        Label roleLabel = new Label("Register as:");
        ToggleGroup roleGroup  = new ToggleGroup();
        RadioButton customerRb = new RadioButton("Customer");
        RadioButton vendorRb   = new RadioButton("Vendor");
        customerRb.setToggleGroup(roleGroup); vendorRb.setToggleGroup(roleGroup);
        customerRb.setSelected(true);
        HBox roleBox = new HBox(15, customerRb, vendorRb);

        String inputStyle = "-fx-text-fill: black; -fx-background-color: white; -fx-prompt-text-fill: #999;";
        TextField   usernameField       = new TextField();
        usernameField.setPromptText("Full Name (cannot start with a digit)");
        usernameField.setStyle(inputStyle);
        TextField   emailFieldDialog    = new TextField();
        emailFieldDialog.setPromptText("name@example.com");
        emailFieldDialog.setStyle(inputStyle);

        PasswordField passwordFieldDialog   = new PasswordField();
        passwordFieldDialog.setPromptText("Min 8 chars, A-Z, a-z, 0-9, symbol");
        passwordFieldDialog.setStyle(inputStyle);
        TextField     passwordVisibleDialog = new TextField();
        passwordVisibleDialog.setStyle(inputStyle);
        passwordVisibleDialog.setVisible(false); passwordVisibleDialog.setManaged(false);
        final boolean[] showPw = {false};
        Button eyeBtn = new Button("👁");
        eyeBtn.setStyle("-fx-cursor: hand;");
        eyeBtn.setOnAction(e -> {
            showPw[0] = !showPw[0];
            if (showPw[0]) {
                passwordVisibleDialog.setText(passwordFieldDialog.getText());
                passwordFieldDialog.setVisible(false); passwordFieldDialog.setManaged(false);
                passwordVisibleDialog.setVisible(true); passwordVisibleDialog.setManaged(true); eyeBtn.setText("🙈");
            } else {
                passwordFieldDialog.setText(passwordVisibleDialog.getText());
                passwordVisibleDialog.setVisible(false); passwordVisibleDialog.setManaged(false);
                passwordFieldDialog.setVisible(true); passwordFieldDialog.setManaged(true); eyeBtn.setText("👁");
            }
        });
        HBox passwordBox = new HBox(5, passwordFieldDialog, passwordVisibleDialog, eyeBtn);
        Label passwordHint = new Label("Must have: uppercase, lowercase, digit & special character (min 8)");
        passwordHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        TextField vendorNameField  = new TextField();
        vendorNameField.setPromptText("Your Vendor Name (unique)");
        vendorNameField.setStyle(inputStyle);
        TextField vendorPhoneField = new TextField();
        // FIX 3: phone hint updated
        vendorPhoneField.setPromptText("Phone starting with 01 (e.g. 01012345678)");
        vendorPhoneField.setStyle(inputStyle);
        Label vendorNote = new Label("Vendor accounts require manager approval before you can log in.");
        vendorNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #e67e22;");

        VBox vendorExtras = new VBox(5,
            new Label("Vendor Name:"), vendorNameField,
            new Label("Phone:"), vendorPhoneField,
            vendorNote);
        vendorExtras.setVisible(false); vendorExtras.setManaged(false);

        vendorRb.selectedProperty().addListener((obs, was, isNow) -> {
            vendorExtras.setVisible(isNow); vendorExtras.setManaged(isNow);
            dialog.getDialogPane().getScene().getWindow().sizeToScene();
        });

        // FIX 4: errorMsg label is INSIDE the dialog, not errorLabel on login screen
        Label errorMsg = new Label("");
        errorMsg.setStyle("-fx-text-fill: red; -fx-font-size: 12px; -fx-wrap-text: true;");
        errorMsg.setMaxWidth(400);

        VBox vbox = new VBox(10,
            roleLabel, roleBox,
            new Label("Username:"), usernameField,
            new Label("Email:"),    emailFieldDialog,
            new Label("Password:"), passwordBox, passwordHint,
            vendorExtras,
            errorMsg);
        vbox.setPadding(new Insets(15));
        vbox.setPrefWidth(480);

        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String username = usernameField.getText().trim();
            String email    = emailFieldDialog.getText().trim();
            String password = showPw[0] ? passwordVisibleDialog.getText() : passwordFieldDialog.getText();
            boolean isVendor = vendorRb.isSelected();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                errorMsg.setText("Please fill in all required fields."); event.consume(); return;
            }
            if (!isUsernameValid(username)) {
                errorMsg.setText("Username cannot start with a digit."); event.consume(); return;
            }
            if (!isEmailValid(email)) {
                errorMsg.setText("Email format: name@domain.com / name@msa.edu.eg / etc."); event.consume(); return;
            }
            if (!isPasswordValid(password)) {
                errorMsg.setText("Password: 8+ chars, 1 uppercase, 1 lowercase, 1 digit, 1 special char."); event.consume(); return;
            }
            if (isVendor) {
                String vname  = vendorNameField.getText().trim();
                String vphone = vendorPhoneField.getText().trim();
                if (vname.isEmpty() || vphone.isEmpty()) {
                    errorMsg.setText("Please enter your vendor name and phone number."); event.consume(); return;
                }
                if (!isUsernameValid(vname)) {
                    errorMsg.setText("Vendor name cannot start with a digit."); event.consume(); return;
                }
                // FIX 3 + FIX 4: phone validation with 01 rule shown inside dialog
                if (!isPhoneValid(vphone)) {
                    errorMsg.setText("Phone must start with 01 and be exactly 11 digits (e.g. 01012345678)."); event.consume(); return;
                }
            }
        });

        // Apply dark theme to dialog
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        dialog.getDialogPane().setStyle("-fx-background-color: #1a0a2e;");

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String username = usernameField.getText().trim();
                String email    = emailFieldDialog.getText().trim();
                String password = showPw[0] ? passwordVisibleDialog.getText() : passwordFieldDialog.getText();
                boolean isVendor = vendorRb.isSelected();

                // ── Send email verification code BEFORE writing to DB ───────────
                UserDAO.sendEmailVerificationCode(email, username);
                showOTPVerificationThenRegister(username, email, password, isVendor,
                        isVendor ? vendorNameField.getText().trim() : null,
                        isVendor ? (vendorPhoneField.getText().trim().isEmpty() ? null : vendorPhoneField.getText().trim()) : null,
                        errorMsg);
            }
        });
    }

    /**
     * Shows an OTP dialog; only calls the DAO to register once the code is confirmed.
     */
    private void showOTPVerificationThenRegister(String username, String email, String password,
                                                  boolean isVendor, String vendorName,
                                                  String vendorPhone, Label errorMsg) {
        String inputStyle = "-fx-text-fill: black; -fx-background-color: white; -fx-prompt-text-fill: #999;";

        Label info = new Label("A 6-digit verification code was sent to:\n" + email);
        info.setStyle("-fx-font-size:12px;-fx-text-fill:#aaa;-fx-wrap-text:true;");

        TextField otpField = new TextField();
        otpField.setPromptText("Enter the 6-digit code");
        otpField.setStyle(inputStyle);

        Label errLbl = new Label("");
        errLbl.setStyle("-fx-text-fill:red;-fx-font-size:12px;-fx-wrap-text:true;");
        errLbl.setMaxWidth(380);

        VBox content = new VBox(10, info, new Label("Verification Code:"), otpField, errLbl);
        content.setPadding(new Insets(15));
        content.setPrefWidth(400);

        Dialog<ButtonType> otpDialog = new Dialog<>();
        otpDialog.setTitle("Verify Your Email");
        otpDialog.setHeaderText("Email Verification");
        otpDialog.getDialogPane().setContent(content);
        otpDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleDialog(otpDialog.getDialogPane());

        Button okBtn = (Button) otpDialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (!UserDAO.validateOTP(email, otpField.getText().trim())) {
                errLbl.setText("⚠ Invalid or expired code. Please try again.");
                ev.consume();
            }
        });

        otpDialog.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                // OTP was valid — now actually register
                if (isVendor) {
                    VendorDAO.registerVendor(username, email, password, vendorName, vendorPhone,
                        () -> Platform.runLater(() -> showRegistrationSuccess(otpDialog,
                            "Vendor registered! Awaiting manager approval.\nYou can log in once the manager approves your account.")),
                        e -> Platform.runLater(() -> {
                            String msg = getFriendlyError(e.getMessage());
                            errorMsg.setText(msg);
                            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                            alert.setTitle("Registration Failed");
                            alert.setHeaderText("Could not create vendor account");
                            alert.setContentText(msg);
                            alert.showAndWait();
                        }));
                } else {
                    UserDAO.register(username, email, password, "CUSTOMER",
                        () -> Platform.runLater(() -> showRegistrationSuccess(otpDialog,
                            "Email verified! Account created successfully. Please log in.")),
                        e -> Platform.runLater(() -> {
                            String msg = getFriendlyError(e.getMessage());
                            errorMsg.setText(msg);
                            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                            alert.setTitle("Registration Failed");
                            alert.setHeaderText("Could not create account");
                            alert.setContentText(msg);
                            alert.showAndWait();
                        }));
                }
            }
        });
    }

    // Fix 15: returns friendly duplicate-error message to be shown inside the create account dialog
    private String getFriendlyError(String msg) {
        if (msg == null) return "Registration failed. Please try again.";
        if (msg.toLowerCase().contains("email") || msg.toLowerCase().contains("user.email"))
            return "⚠ This email address is already registered. Please use a different email.";
        else if (msg.toLowerCase().contains("companyname") || msg.toLowerCase().contains("vendor.companyname") || msg.toLowerCase().contains("vendor name"))
            return "⚠ This vendor name is already taken. Please choose a unique vendor name.";
        else if (msg.toLowerCase().contains("phonenumber") || msg.toLowerCase().contains("phone"))
            return "⚠ This phone number is already registered. Please use a different phone number.";
        else if (msg.toLowerCase().contains("duplicate") || msg.toLowerCase().contains("unique"))
            return "⚠ A record with this information already exists. Please check your email, phone, or vendor name.";
        return "⚠ Registration failed: " + msg;
    }

    private void showRegistrationSuccess(Dialog<?> dialog, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Registration Successful");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ── Applies the dark theme to any dialog pane ─────────────────────────────
    private void styleDialog(DialogPane pane) {
        try {
            pane.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        } catch (Exception ignored) {}
        pane.setStyle("-fx-background-color: #1a0a2e;");
    }
}
