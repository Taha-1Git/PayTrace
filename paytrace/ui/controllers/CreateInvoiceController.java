package com.paytrace.ui.controllers;

import com.paytrace.dao.UserConnectionDAO;
import com.paytrace.dao.UserDAO;
import com.paytrace.models.Invoice;
import com.paytrace.models.User;
import com.paytrace.models.UserConnection;
import com.paytrace.models.enums.InvoiceStatus;
import com.paytrace.services.EmailService;
import com.paytrace.services.InvoiceService;
import com.paytrace.services.InvoiceService;
import com.paytrace.ui.utils.SceneNavigator;
import com.paytrace.ui.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CreateInvoiceController {

    @FXML private ComboBox<String> userBox;
    @FXML private TextField invoiceNumberField;
    @FXML private TextField amountField;
    @FXML private DatePicker dueDatePicker;
    @FXML private TextArea  descriptionField;
    @FXML private Label     statusLabel;

    private final InvoiceService        invoiceService = new InvoiceService();
    private final UserConnectionDAO connDAO        = new UserConnectionDAO();
    private final UserDAO           userDAO        = new UserDAO();

    // display label → User object
    private final Map<String, User> userByLabel = new HashMap<>();

    @FXML
    public void initialize() {
        SessionManager sm = SessionManager.getInstance();

        // Default due date = today + 30 days
        dueDatePicker.setValue(LocalDate.now().plusDays(30));

        // Auto-generate an invoice number on load
        invoiceNumberField.setText(suggestInvoiceNumber());

        loadConnectedUsers();
    }

    /** Loads all users who have a connection to this administrator's vendor. */
    private void loadConnectedUsers() {
        userBox.getItems().clear();
        userByLabel.clear();
        try {
            String vendorId = SessionManager.getInstance().getContextId();
            if (vendorId == null) {
                statusLabel.setText("No vendor context. Please re-login.");
                statusLabel.setStyle("-fx-text-fill: #dc2626;");
                return;
            }

            List<UserConnection> conns = connDAO.findByTarget(vendorId);
            if (conns.isEmpty()) {
                statusLabel.setText("No users have connected to your vendor yet. " +
                        "Approve some connection requests first.");
                statusLabel.setStyle("-fx-text-fill: #d97706;");
                return;
            }

            for (UserConnection uc : conns) {
                Optional<User> u = userDAO.findById(uc.getUserId());
                if (u.isEmpty()) continue;
                String label = u.get().getFullName() + " (" + u.get().getEmail() + ")";
                userBox.getItems().add(label);
                userByLabel.put(label, u.get());
            }
        } catch (Exception e) {
            statusLabel.setText("Failed to load users: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    private void generateInvoiceNumber() {
        invoiceNumberField.setText(suggestInvoiceNumber());
    }

    private String suggestInvoiceNumber() {
        long ts = System.currentTimeMillis() % 100000;
        return "INV-" + LocalDate.now().getYear() + "-" + String.format("%05d", ts);
    }

    @FXML
    private void createInvoice() {
        statusLabel.setText("");
        statusLabel.setStyle(null);

        String userLabel = userBox.getValue();
        String invNum    = invoiceNumberField.getText() == null
                ? "" : invoiceNumberField.getText().trim();
        String amtStr    = amountField.getText() == null
                ? "" : amountField.getText().trim();
        LocalDate due    = dueDatePicker.getValue();
        String desc      = descriptionField.getText() == null
                ? "" : descriptionField.getText().trim();

        // Validation
        if (userLabel == null) { showError("Pick a user."); return; }
        if (invNum.isEmpty())   { showError("Invoice number required."); return; }
        if (amtStr.isEmpty())   { showError("Amount required."); return; }
        if (due == null)        { showError("Due date required."); return; }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amtStr);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Amount must be a positive number.");
            return;
        }

        User targetUser = userByLabel.get(userLabel);
        if (targetUser == null) { showError("User not found."); return; }

        try {
            String vendorId = SessionManager.getInstance().getContextId();
            String vendorName = SessionManager.getInstance().getContextName();

            Invoice inv = new Invoice();
            inv.setInvoiceNumber(invNum);
            inv.setOwnerType("VENDOR");
            inv.setOwnerId(vendorId);
            inv.setUserId(targetUser.getUserId());
            inv.setCounterparty(targetUser.getFullName());
            inv.setDescription(desc);
            inv.setAmountDue(amount);
            inv.setRemainingBalance(amount);
            inv.setStatus(InvoiceStatus.APPROVED);  // auto-approved by issuer
            inv.setDueDate(due);
            // Through the service: validation + audit log + DAO persist.
            invoiceService.createInvoice(inv);

            // Send email in a background thread
            String to    = targetUser.getEmail();
            String name  = targetUser.getFullName();
            String subj  = "PayTrace — New invoice from " + vendorName;
            String body  =
                    "Hello " + name + ",\n\n" +
                            vendorName + " has issued you a new invoice:\n\n" +
                            "  Invoice #: " + invNum + "\n" +
                            "  Amount:    PKR " + amount.toPlainString() + "\n" +
                            "  Due Date:  " + due + "\n" +
                            "  For:       " + (desc.isEmpty() ? "(no description)" : desc) + "\n\n" +
                            "Please log into PayTrace to view it under 'My Invoices' " +
                            "and pay it before the due date.\n\n" +
                            "— PayTrace";

            new Thread(() -> {
                try {
                    EmailService.send(to, subj, body);
                    Platform.runLater(() -> {
                        statusLabel.setText("✓ Invoice created and emailed to " + to);
                        statusLabel.setStyle("-fx-text-fill: #16a34a;");
                    });
                } catch (Exception mailEx) {
                    Platform.runLater(() -> {
                        statusLabel.setText(
                                "Invoice saved, but email failed: " + mailEx.getMessage());
                        statusLabel.setStyle("-fx-text-fill: #d97706;");
                    });
                }
            }, "invoice-email").start();

            // Clear form for next entry
            statusLabel.setText("Saving invoice & sending email...");
            statusLabel.setStyle("-fx-text-fill: #2563eb;");
            amountField.clear();
            descriptionField.clear();
            invoiceNumberField.setText(suggestInvoiceNumber());
        } catch (Exception e) {
            showError("Failed to save invoice: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #dc2626;");
    }

    @FXML
    private void goBack() {
        try { SceneNavigator.navigateTo(SceneNavigator.DASHBOARD, 1280, 800); }
        catch (Exception e) { e.printStackTrace(); }
    }
}