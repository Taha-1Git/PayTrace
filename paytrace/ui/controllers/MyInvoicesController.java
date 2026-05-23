package com.paytrace.ui.controllers;

import com.paytrace.dao.UserDAO;
import com.paytrace.dao.VendorDAO;
import com.paytrace.models.Invoice;
import com.paytrace.models.Notification;
import com.paytrace.models.Payment;
import com.paytrace.models.User;
import com.paytrace.models.Vendor;
import com.paytrace.services.InvoiceService;
import com.paytrace.services.NotificationService;
import com.paytrace.services.PaymentService;
import com.paytrace.services.InvoiceService;
import com.paytrace.services.PaymentService;
import com.paytrace.ui.utils.SceneNavigator;
import com.paytrace.ui.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class MyInvoicesController {

    @FXML private Label totalCountLabel;
    @FXML private Label unpaidCountLabel;
    @FXML private Label paidCountLabel;
    @FXML private Label totalOwedLabel;

    @FXML private TableView<Invoice> invoiceTable;
    @FXML private TableColumn<Invoice, String> numberCol;
    @FXML private TableColumn<Invoice, String> descCol;
    @FXML private TableColumn<Invoice, String> amountCol;
    @FXML private TableColumn<Invoice, String> balanceCol;
    @FXML private TableColumn<Invoice, String> dueDateCol;
    @FXML private TableColumn<Invoice, String> statusCol;

    @FXML private Label actionStatusLabel;

    /** Business-logic boundary — see services/InvoiceService.java */
    private final InvoiceService invoiceService = new InvoiceService();
    private final PaymentService paymentService = new PaymentService();
    private final VendorDAO  vendorDAO      = new VendorDAO();
    private final UserDAO    userDAO        = new UserDAO();

    private final ObservableList<Invoice> rows = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        SessionManager sm = SessionManager.getInstance();

        configureColumns();
        loadInvoices();
    }

    private void configureColumns() {
        numberCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getInvoiceNumber() == null ? "—"
                        : d.getValue().getInvoiceNumber()));
        descCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDescription() == null ? ""
                        : d.getValue().getDescription()));
        amountCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getAmountDue() != null
                        ? d.getValue().getAmountDue().toPlainString() : "0"));
        balanceCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getRemainingBalance() != null
                        ? d.getValue().getRemainingBalance().toPlainString() : "—"));
        dueDateCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDueDate() == null ? "—"
                        : d.getValue().getDueDate().toString()));
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getStatus() == null ? "Unknown" : d.getValue().getStatus().name()
        ));

        invoiceTable.setItems(rows);
    }

    @FXML
    private void loadInvoices() {
        try {
            SessionManager sm = SessionManager.getInstance();
            String userId   = sm.getCurrentUserId();
            String vendorId = sm.getContextId();

            if (userId == null) { actionStatusLabel.setText("No user session."); return; }
            if (vendorId == null) {
                actionStatusLabel.setText(
                        "No vendor selected. Log in with a vendor code first.");
                return;
            }

            List<Invoice> mine = invoiceService.findByOwnerAndUser(vendorId, userId);
            rows.setAll(mine);
            updateKPIs(mine);
            actionStatusLabel.setText(mine.size() + " invoice(s).");
            actionStatusLabel.setStyle(null);
        } catch (Exception e) {
            actionStatusLabel.setText("Load failed: " + e.getMessage());
            actionStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    private void updateKPIs(List<Invoice> list) {
        int total = list.size();
        int paid = 0, unpaid = 0;
        BigDecimal owed = BigDecimal.ZERO;

        for (Invoice inv : list) {
            BigDecimal rem = inv.getRemainingBalance();
            if (rem != null && rem.compareTo(BigDecimal.ZERO) <= 0) {
                paid++;
            } else {
                unpaid++;
                if (rem != null) owed = owed.add(rem);
            }
        }

        totalCountLabel.setText(String.valueOf(total));
        unpaidCountLabel.setText(String.valueOf(unpaid));
        paidCountLabel.setText(String.valueOf(paid));
        totalOwedLabel.setText(owed.toPlainString());
    }

    /** Triggered by the "Pay Bill" button (renamed from openUploadProof). */
    @FXML
    private void openUploadProof() {
        Invoice sel = invoiceTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            actionStatusLabel.setText("Select an invoice first.");
            actionStatusLabel.setStyle("-fx-text-fill: #d97706;");
            return;
        }
        BigDecimal rem = sel.getRemainingBalance();
        if (rem == null || rem.compareTo(BigDecimal.ZERO) <= 0) {
            actionStatusLabel.setText("This invoice is already paid in full.");
            actionStatusLabel.setStyle("-fx-text-fill: #16a34a;");
            return;
        }

        showPayDialog(sel);
    }

    /** The Pay Bill dialog — user enters amount, confirms, payment is recorded. */
    private void showPayDialog(Invoice inv) {
        // Get the vendor for display (account number + name)
        Vendor vendor = null;
        try { vendor = vendorDAO.findById(inv.getOwnerId()).orElse(null); }
        catch (Exception ignored) {}
        final Vendor vendorRef = vendor;

        javafx.stage.Stage dlg = new javafx.stage.Stage();
        dlg.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dlg.setTitle("Pay Bill — " + inv.getInvoiceNumber());

        VBox root = new VBox(12);
        root.setPadding(new javafx.geometry.Insets(20));
        root.setStyle("-fx-background-color: white;");
        root.setPrefWidth(440);

        Label header = new Label("Pay Invoice " + inv.getInvoiceNumber());
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox infoBox = new VBox(4);
        infoBox.setStyle("-fx-padding: 12; -fx-background-color: #f1f5f9; " +
                "-fx-background-radius: 6;");
        infoBox.getChildren().addAll(
                new Label("Vendor: " + (vendorRef != null ? vendorRef.getVendorName() : "—")),
                new Label("Vendor Account: " + (vendorRef != null
                        ? vendorRef.getAccountNumber() : "—")),
                new Label("Original Amount: PKR " +
                        (inv.getAmountDue() != null ? inv.getAmountDue().toPlainString() : "—")),
                new Label("Remaining Balance: PKR " + inv.getRemainingBalance().toPlainString()),
                new Label("Description: " + (inv.getDescription() == null
                        ? "—" : inv.getDescription()))
        );

        Label amtLbl = new Label("Amount to Pay (PKR):");
        TextField amtField = new TextField();
        amtField.setText(inv.getRemainingBalance().toPlainString());
        amtField.setPromptText("Up to " + inv.getRemainingBalance().toPlainString());

        Label hint = new Label("Tip: You can pay the full balance or any partial amount.");
        hint.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        Label dlgStatus = new Label();
        dlgStatus.setWrapText(true);

        Button pay = new Button("💳 Confirm Payment");
        pay.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-padding: 10 20;");
        Button cancel = new Button("Cancel");
        HBox btns = new HBox(8, pay, cancel);

        cancel.setOnAction(e -> dlg.close());

        pay.setOnAction(e -> {
            BigDecimal amt;
            try {
                amt = new BigDecimal(amtField.getText().trim());
                if (amt.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            } catch (Exception ex) {
                dlgStatus.setText("Amount must be a positive number.");
                dlgStatus.setStyle("-fx-text-fill: #dc2626;");
                return;
            }
            if (amt.compareTo(inv.getRemainingBalance()) > 0) {
                dlgStatus.setText("You cannot pay more than the remaining balance ("
                        + inv.getRemainingBalance().toPlainString() + ").");
                dlgStatus.setStyle("-fx-text-fill: #dc2626;");
                return;
            }

            try {
                processPayment(inv, vendorRef, amt);
                dlg.close();
            } catch (Exception ex) {
                dlgStatus.setText("Payment failed: " + ex.getMessage());
                dlgStatus.setStyle("-fx-text-fill: #dc2626;");
            }
        });

        root.getChildren().addAll(header, infoBox, amtLbl, amtField, hint,
                dlgStatus, btns);
        dlg.setScene(new javafx.scene.Scene(root));
        dlg.showAndWait();
    }

    /** Records the payment, updates invoice balance, fires notifications. */
    private void processPayment(Invoice inv, Vendor vendor, BigDecimal amount)
            throws Exception {
        SessionManager sm = SessionManager.getInstance();
        String userId = sm.getCurrentUserId();
        Optional<User> uOpt = userDAO.findById(userId);
        String userName  = uOpt.map(User::getFullName).orElse("User");

        // 1+2. Record the payment via PaymentService (handles txn-id generation,
        //      validation, and DAO persistence in one call).
        Payment p = paymentService.recordFullyAllocatedPayment(
                inv.getOwnerId(),
                userName,
                vendor != null ? vendor.getAccountNumber() : null,
                amount);
        String txnRef = p.getTransactionId();

        // 3. Update invoice's remaining balance via InvoiceService.
        BigDecimal newBalance = inv.getRemainingBalance().subtract(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) newBalance = BigDecimal.ZERO;
        invoiceService.updateRemainingBalance(inv.getInvoiceId(), newBalance);

        boolean fullyPaid = newBalance.compareTo(BigDecimal.ZERO) == 0;

        // 4. Notify the vendor administrator (in-app + email)
        if (vendor != null && vendor.getAdministratorId() != null) {
            String adminMsg = userName + " has paid PKR " + amount.toPlainString() +
                    " for invoice " + inv.getInvoiceNumber() + ".\n\n" +
                    "Remaining balance on this invoice: PKR " + newBalance.toPlainString() +
                    (fullyPaid ? "  ← INVOICE FULLY PAID 🎉" : "") + "\n" +
                    "Transaction reference: " + txnRef;

            NotificationService.notify(
                    vendor.getAdministratorId(),
                    Notification.Type.PAYMENT_RECEIVED,
                    fullyPaid
                            ? "Payment received — invoice " + inv.getInvoiceNumber() + " fully paid"
                            : "Payment received — invoice " + inv.getInvoiceNumber(),
                    adminMsg,
                    "Invoice", inv.getInvoiceId(),
                    true
            );
        }

        // 5. Email the user a confirmation
        String userMsg = "Hello " + userName + ",\n\n" +
                "Your payment has been recorded.\n\n" +
                "Invoice:        " + inv.getInvoiceNumber() + "\n" +
                "Amount paid:    PKR " + amount.toPlainString() + "\n" +
                "Remaining:      PKR " + newBalance.toPlainString() + "\n" +
                "Transaction:    " + txnRef + "\n\n" +
                (fullyPaid
                        ? "🎉 This invoice is now fully paid. Thank you!"
                        : "You can pay the remaining balance anytime from My Invoices.");

        NotificationService.notify(
                userId,
                fullyPaid ? Notification.Type.PAYMENT_COMPLETED
                        : Notification.Type.PAYMENT_RECEIVED,
                fullyPaid
                        ? "Invoice " + inv.getInvoiceNumber() + " fully paid"
                        : "Payment of PKR " + amount.toPlainString() + " confirmed",
                userMsg,
                "Invoice", inv.getInvoiceId(),
                true
        );

        actionStatusLabel.setText("✓ Payment of PKR " + amount.toPlainString() +
                " recorded. " + (fullyPaid ? "Invoice fully paid." :
                "Remaining: PKR " + newBalance.toPlainString()));
        actionStatusLabel.setStyle("-fx-text-fill: #16a34a;");
        com.paytrace.ui.utils.ToastNotification.show(
                invoiceTable.getScene().getWindow(),
                fullyPaid
                        ? "🎉 Invoice " + inv.getInvoiceNumber() + " fully paid"
                        : "✓ Payment of PKR " + amount.toPlainString() + " recorded",
                com.paytrace.ui.utils.ToastNotification.ToastType.SUCCESS);
        loadInvoices();
    }

    @FXML
    private void goBack() {
        try { SceneNavigator.navigateTo(SceneNavigator.DASHBOARD, 1280, 800); }
        catch (Exception e) { e.printStackTrace(); }
    }
}