package com.paytrace.ui.controllers;

import com.paytrace.dao.VendorDAO;
import com.paytrace.models.Session;
import com.paytrace.models.Vendor;
import com.paytrace.models.enums.TargetType;
import com.paytrace.services.AuthService;
import com.paytrace.ui.utils.PendingRequestContext;
import com.paytrace.ui.utils.SceneNavigator;
import com.paytrace.ui.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

public class LoginController {

    @FXML private ComboBox<String> accountTypeBox;
    @FXML private TextField        emailField;
    @FXML private PasswordField    passwordField;

    // Administrator-only
    @FXML private VBox             adminCodeBox;
    @FXML private TextField        adminCodeField;

    // User Option 1
    @FXML private VBox             userCodeBox;
    @FXML private TextField        userCodeField;

    // User Option 2
    @FXML private VBox             userRequestBox;
    @FXML private ComboBox<String> requestTargetBox;

    @FXML private Label            errorLabel;
    @FXML private Button           loginButton;
    @FXML private Hyperlink        registerLink;

    private final AuthService auth      = new AuthService();
    private final VendorDAO   vendorDAO = new VendorDAO();

    // name → id lookup for the request dropdown
    private final Map<String, String> targetNameToId = new HashMap<>();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);

        accountTypeBox.getItems().addAll("Admin", "Administrator", "User");
        accountTypeBox.setValue("User");
        accountTypeBox.setOnAction(e -> updateSectionsVisibility());

        reloadTargets();
        updateSectionsVisibility();

        loginButton.setOnAction(e -> handleLogin());
        registerLink.setOnAction(e -> goToRegister());
    }

    private void updateSectionsVisibility() {
        String type = accountTypeBox.getValue();
        boolean isAdministrator = "Administrator".equals(type);
        boolean isUser  = "User".equals(type);

        adminCodeBox.setVisible(isAdministrator);
        adminCodeBox.setManaged(isAdministrator);

        userCodeBox.setVisible(isUser);
        userCodeBox.setManaged(isUser);
        userRequestBox.setVisible(isUser);
        userRequestBox.setManaged(isUser);
    }

    private void reloadTargets() {
        requestTargetBox.getItems().clear();
        targetNameToId.clear();
        try {
            for (Vendor v : vendorDAO.findAll()) {
                requestTargetBox.getItems().add(v.getVendorName());
                targetNameToId.put(v.getVendorName(), v.getVendorId());
            }
        } catch (Exception ex) {
            // silent — targets list will just be empty
        }
    }

    private void handleLogin() {
        errorLabel.setVisible(false);

        String type     = accountTypeBox.getValue();
        String email    = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText();

        try {
            Session s;
            switch (type) {
                case "Admin": {
                    s = auth.loginAdmin(email, password);
                    SessionManager.getInstance().setCurrentSession(s);
                    SceneNavigator.navigateTo(SceneNavigator.DASHBOARD, 1280, 800);
                    break;
                }
                case "Administrator": {
                    String code = adminCodeField.getText() == null
                            ? "" : adminCodeField.getText().trim();
                    if (code.length() != 4)
                        throw new RuntimeException("Code must be 4 digits.");
                    s = auth.loginAdministrator(email, password, code);
                    SessionManager.getInstance().setCurrentSession(s);
                    SceneNavigator.navigateTo(SceneNavigator.DASHBOARD, 1280, 800);
                    break;
                }
                case "User": {
                    String code = userCodeField.getText() == null
                            ? "" : userCodeField.getText().trim();
                    String pickedTarget = requestTargetBox.getValue();

                    // Option 1 takes priority — user typed a code
                    if (!code.isEmpty()) {
                        if (code.length() != 4)
                            throw new RuntimeException("Code must be 4 digits.");
                        s = auth.loginUser(email, password, code);
                        SessionManager.getInstance().setCurrentSession(s);
                        SceneNavigator.navigateTo(SceneNavigator.DASHBOARD, 1280, 800);
                    }
                    // Option 2 — user picked a vendor to request
                    else if (pickedTarget != null && !pickedTarget.isBlank()) {
                        s = auth.loginUser(email, password, "");   // blank code
                        SessionManager.getInstance().setCurrentSession(s);

                        PendingRequestContext.set(
                                TargetType.VENDOR,
                                targetNameToId.get(pickedTarget),
                                pickedTarget);

                        SceneNavigator.navigateTo(SceneNavigator.CHOOSE_ACCESS, 1000, 700);
                    }
                    // Nothing chosen
                    else {
                        throw new RuntimeException(
                                "Enter your 4-digit code OR pick a vendor to request access.");
                    }
                    break;
                }
                default:
                    throw new RuntimeException("Pick a login type.");
            }
        } catch (Exception ex) {
            showError(ex.getMessage());
            passwordField.clear();
        }
    }

    private void goToRegister() {
        try { SceneNavigator.navigateTo(SceneNavigator.REGISTER); }
        catch (Exception e) { showError("Cannot open register screen."); }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
