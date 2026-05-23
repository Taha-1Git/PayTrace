package com.paytrace.ui.controllers;

import com.paytrace.dao.UserDAO;
import com.paytrace.dao.VendorDAO;
import com.paytrace.models.User;
import com.paytrace.models.Vendor;
import com.paytrace.services.AuthService;
import com.paytrace.ui.utils.EmailSimulator;
import com.paytrace.ui.utils.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class RegisterController {

    @FXML private ComboBox<String> typeBox;
    @FXML private TextField        nameField;
    @FXML private TextField        emailField;
    @FXML private PasswordField    passwordField;
    @FXML private PasswordField    confirmField;

    @FXML private VBox             adminFields;
    @FXML private TextField        entityNameField;
    @FXML private TextField        entityEmailField;
    @FXML private TextField        entityAddressField;

    @FXML private Label            errorLabel;
    @FXML private Label            successLabel;
    @FXML private Button           registerButton;
    @FXML private Hyperlink        backToLoginLink;

    private final AuthService auth     = new AuthService();
    private final UserDAO     userDAO  = new UserDAO();
    private final VendorDAO   vendorDAO = new VendorDAO();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);

        typeBox.getItems().addAll("User", "Administrator");
        typeBox.setValue("User");
        typeBox.setOnAction(e -> updateAdminVisibility());

        updateAdminVisibility();

        registerButton.setOnAction(e -> handleRegister());
        backToLoginLink.setOnAction(e -> goToLogin());
    }

    private void updateAdminVisibility() {
        boolean isAdmin = "Administrator".equals(typeBox.getValue());
        adminFields.setVisible(isAdmin);
        adminFields.setManaged(isAdmin);
    }

    private void handleRegister() {
        String name    = nameField.getText() == null ? "" : nameField.getText().trim();
        String email   = emailField.getText() == null ? "" : emailField.getText().trim();
        String pwd     = passwordField.getText();
        String confirm = confirmField.getText();

        if (!pwd.equals(confirm)) { showError("Passwords do not match."); return; }

        try {
            if ("User".equals(typeBox.getValue())) {
                auth.registerUser(name, email, pwd);
                showSuccess("Account created. You can now log in as User.");
            } else {
                String entName = entityNameField.getText();
                String entMail = entityEmailField.getText();
                String entAddr = entityAddressField.getText();

                auth.registerAdministratorWithVendor(name, email, pwd, entName, entMail, entAddr);

                // Look up the created vendor to display its 4-digit access code
                Optional<User> savedUser = userDAO.findByEmail(email.trim().toLowerCase());
                String code = "—";
                if (savedUser.isPresent()) {
                    Optional<Vendor> v = vendorDAO.findByAdministratorId(savedUser.get().getUserId());
                    if (v.isPresent()) code = v.get().getAccessCode();
                }

                String emailBody = "Hello " + name + ",\n\n" +
                        "Your vendor '" + entName + "' has been registered.\n\n" +
                        "Your 4-digit administrator code is: " + code + "\n\n" +
                        "Use this code along with your email + password every time you log in.\n" +
                        "Share this code with users who request to connect to your vendor.\n\n" +
                        "— PayTrace";

                // Show popup with access code (in case SMTP isn't configured)
                EmailSimulator.send(email,
                        "Your PayTrace Vendor has been created", emailBody);

                // Also fire a real email in the background — silent if SMTP not set up
                final String mailBody = emailBody;
                new Thread(() -> {
                    try {
                        com.paytrace.services.EmailService.send(email,
                                "Your PayTrace Vendor has been created", mailBody);
                    } catch (Exception ignore) { /* SMTP optional */ }
                }, "register-email").start();

                showSuccess("Administrator account created. Your code is " + code + ".");
            }
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void goToLogin() {
        try { SceneNavigator.navigateTo(SceneNavigator.LOGIN); }
        catch (Exception e) { showError("Cannot open login screen."); }
    }

    private void showError(String m) {
        errorLabel.setText(m); errorLabel.setVisible(true);
        successLabel.setVisible(false);
    }
    private void showSuccess(String m) {
        successLabel.setText(m); successLabel.setVisible(true);
        errorLabel.setVisible(false);
    }
}
