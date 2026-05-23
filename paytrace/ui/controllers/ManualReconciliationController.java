package com.paytrace.ui.controllers;

import com.paytrace.dao.UserDAO;
import com.paytrace.models.Invoice;
import com.paytrace.models.Notification;
import com.paytrace.models.Payment;
import com.paytrace.models.ReconciliationMatch;
import com.paytrace.models.User;
import com.paytrace.models.UserConnection;
import com.paytrace.models.enums.MatchType;
import com.paytrace.models.enums.PaymentStatus;
import com.paytrace.services.InvoiceService;
import com.paytrace.services.NotificationService;
import com.paytrace.services.PaymentService;
import com.paytrace.services.ReconciliationService;
import com.paytrace.services.ReportService;
import com.paytrace.ui.utils.SceneNavigator;
import com.paytrace.ui.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ManualReconciliationController {




    @FXML private Button dueDateCheckButton;
    @FXML private Label  dueDateStatusLabel;

    @FXML private TableView<Invoice> invoiceTable;
    @FXML private TableColumn<Invoice, String> invNumCol;
    @FXML private TableColumn<Invoice, String> invUserCol;
    @FXML private TableColumn<Invoice, String> invAmountCol;
    @FXML private TableColumn<Invoice, String> invDateCol;

    @FXML private TableView<Payment> paymentTable;
    @FXML private TableColumn<Payment, String> payTxnCol;
    @FXML private TableColumn<Payment, String> paySenderCol;
    @FXML private TableColumn<Payment, String> payAmountCol;
    @FXML private TableColumn<Payment, String> payDateCol;

    @FXML private Label checkAccountLabel;
    @FXML private Label checkAmountLabel;
    @FXML private Label checkDateLabel;
    @FXML private Label manualStatusLabel;

    /** All writes flow through services so audit log + Command pattern run consistently. */
    private final InvoiceService        invoiceService        = new InvoiceService();
    private final PaymentService        paymentService        = new PaymentService();
    private final ReconciliationService reconciliationService = new ReconciliationService();
    private final ReportService         reportService         = new ReportService();
    /** Retained for two narrow user-account ops (lookup name, block). */
    private final UserDAO           userDAO               = new UserDAO();

    private final ObservableList<Invoice> invoiceRows = FXCollections.observableArrayList();
    private final ObservableList<Payment> paymentRows = FXCollections.observableArrayList();
    private final Map<String, String> userIdToName = new HashMap<>();

    @FXML
    public void initialize() {
        SessionManager sm = SessionManager.getInstance();

        configureColumns();
        loadData();

        invoiceTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> updateCrossCheck());
        paymentTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> updateCrossCheck());
    }

    private void configureColumns() {
        invNumCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getInvoiceNumber()));
        invUserCol.setCellValueFactory(d ->
                new SimpleStringProperty(resolveUserName(d.getValue().getUserId())));
        invAmountCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getRemainingBalance() == null
                        ? "0" : d.getValue().getRemainingBalance().toPlainString()));
        invDateCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getCreatedAt() == null ? "—"
                        : d.getValue().getCreatedAt().toLocalDate().toString()));

        payTxnCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTransactionId()));
        paySenderCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getSenderAccount() == null ? "—"
                        : d.getValue().getSenderAccount()));
        payAmountCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getAmountPaid() == null ? "0"
                        : d.getValue().getAmountPaid().toPlainString()));
        payDateCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getPaymentDate() == null ? "—"
                        : d.getValue().getPaymentDate().toString()));

        invoiceTable.setItems(invoiceRows);
        paymentTable.setItems(paymentRows);
    }

    private String resolveUserName(String userId) {
        if (userId == null) return "—";
        if (userIdToName.containsKey(userId)) return userIdToName.get(userId);
        try {
            Optional<User> u = userDAO.findById(userId);
            String name = u.map(User::getFullName).orElse("—");
            userIdToName.put(userId, name);
            return name;
        } catch (Exception e) { return "—"; }
    }

    private void loadData() {
        try {
            String vendorId = SessionManager.getInstance().getContextId();
            if (vendorId == null) return;

            invoiceRows.clear();
            for (Invoice i : invoiceService.findByOwner(vendorId)) {
                if (i.getRemainingBalance() != null
                        && i.getRemainingBalance().compareTo(BigDecimal.ZERO) > 0) {
                    invoiceRows.add(i);
                }
            }

            paymentRows.clear();
            for (Payment p : paymentService.findByOwner(vendorId)) {
                PaymentStatus st = p.getStatus();
                if (st == PaymentStatus.UNMATCHED || st == PaymentStatus.MANUAL_REVIEW) {
                    paymentRows.add(p);
                }
            }
        } catch (Exception e) {
            manualStatusLabel.setText("Load failed: " + e.getMessage());
        }
    }

    private void updateCrossCheck() {
        Invoice inv = invoiceTable.getSelectionModel().getSelectedItem();
        Payment pay = paymentTable.getSelectionModel().getSelectedItem();
        if (inv == null || pay == null) {
            checkAccountLabel.setText("—");
            checkAmountLabel.setText("—");
            checkDateLabel.setText("—");
            return;
        }

        // Account check
        try {
            String vendorId = SessionManager.getInstance().getContextId();
            String registeredAccount = null;
            for (UserConnection uc : reportService.vendorConnections(vendorId)) {
                if (uc.getUserId().equals(inv.getUserId())) {
                    registeredAccount = uc.getUserAccountNumber();
                    break;
                }
            }
            boolean accountMatch = registeredAccount != null
                    && registeredAccount.equals(pay.getSenderAccount());
            checkAccountLabel.setText(accountMatch ? "✓" : "✗");
            checkAccountLabel.setStyle(accountMatch
                    ? "-fx-text-fill: #16a34a;" : "-fx-text-fill: #dc2626;");
        } catch (Exception e) {
            checkAccountLabel.setText("?");
        }

        // Amount check (within 1.00 tolerance)
        if (inv.getRemainingBalance() != null && pay.getAmountPaid() != null) {
            BigDecimal diff = inv.getRemainingBalance().subtract(pay.getAmountPaid()).abs();
            boolean amountMatch = diff.compareTo(BigDecimal.ONE) <= 0;
            checkAmountLabel.setText(amountMatch ? "✓" : "✗");
            checkAmountLabel.setStyle(amountMatch
                    ? "-fx-text-fill: #16a34a;" : "-fx-text-fill: #dc2626;");
        }

        // Date check (within 7 days)
        if (inv.getCreatedAt() != null && pay.getPaymentDate() != null) {
            long days = Math.abs(ChronoUnit.DAYS.between(
                    inv.getCreatedAt().toLocalDate(), pay.getPaymentDate()));
            boolean dateMatch = days <= 7;
            checkDateLabel.setText(dateMatch ? "✓ (" + days + "d)" : "✗ (" + days + "d)");
            checkDateLabel.setStyle(dateMatch
                    ? "-fx-text-fill: #16a34a;" : "-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    private void confirmManualMatch() {
        Invoice inv = invoiceTable.getSelectionModel().getSelectedItem();
        Payment pay = paymentTable.getSelectionModel().getSelectedItem();
        if (inv == null || pay == null) {
            manualStatusLabel.setText("Select one invoice and one payment first.");
            manualStatusLabel.setStyle("-fx-text-fill: #d97706;");
            return;
        }

        javafx.scene.control.Alert a = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm Manual Match");
        a.setHeaderText("Match payment TXN-" + pay.getTransactionId()
                + " to invoice " + inv.getInvoiceNumber() + "?");
        a.setContentText("Amount paid: " + pay.getAmountPaid()
                + "\nInvoice balance: " + inv.getRemainingBalance());
        Optional<ButtonType> r = a.showAndWait();
        if (r.isEmpty() || r.get() != ButtonType.OK) return;

        try {
            // Build the match payload — service wraps it in a MatchCommand
            // (Command pattern, undoable) and writes the audit entry.
            ReconciliationMatch m = new ReconciliationMatch();
            m.setInvoiceId(inv.getInvoiceId());
            m.setPaymentId(pay.getPaymentId());
            m.setMatchType(MatchType.MANUAL);
            m.setConfidenceScore(1.0);
            m.setExplanation("Manually matched by administrator");
            m.setReversed(false);
            m.setMatchedAt(LocalDateTime.now());

            BigDecimal newBal = inv.getRemainingBalance().subtract(pay.getAmountPaid());
            if (newBal.compareTo(BigDecimal.ZERO) < 0) newBal = BigDecimal.ZERO;

            String adminId = SessionManager.getInstance().getCurrentUserId();
            reconciliationService.persistManualMatch(m,
                    inv.getInvoiceId(), pay.getPaymentId(),
                    inv.getRemainingBalance(), newBal, adminId);
            paymentService.updateStatus(pay.getPaymentId(), PaymentStatus.FULLY_ALLOCATED);

            // Notify user
            if (inv.getUserId() != null) {
                NotificationService.notify(inv.getUserId(),
                        Notification.Type.RECONCILIATION_RESULT,
                        "Payment manually verified — " + inv.getInvoiceNumber(),
                        "Your payment has been manually verified by the administrator.\n"
                                + "Invoice: " + inv.getInvoiceNumber()
                                + "\nAmount: " + pay.getAmountPaid()
                                + "\nRemaining: " + newBal,
                        "Invoice", inv.getInvoiceId(), true);
            }

            manualStatusLabel.setText("✓ Manual match confirmed.");
            manualStatusLabel.setStyle("-fx-text-fill: #16a34a;");
            com.paytrace.ui.utils.ToastNotification.show(
                    invoiceTable.getScene().getWindow(),
                    "✓ Match confirmed — user notified",
                    com.paytrace.ui.utils.ToastNotification.ToastType.SUCCESS);
            loadData();
        } catch (Exception ex) {
            manualStatusLabel.setText("Failed: " + ex.getMessage());
            manualStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    private void runDueDateCheck() {
        try {
            String vendorId = SessionManager.getInstance().getContextId();
            if (vendorId == null) return;

            List<Invoice> all = invoiceService.findByOwner(vendorId);
            int warned = 0, blocked = 0;
            LocalDate today = LocalDate.now();

            for (Invoice inv : all) {
                if (inv.getRemainingBalance() == null
                        || inv.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) continue;
                if (inv.getDueDate() == null) continue;

                long daysUntilDue = ChronoUnit.DAYS.between(today, inv.getDueDate());

                if (daysUntilDue >= 0 && daysUntilDue <= 7) {
                    // Warning email
                    if (inv.getUserId() != null) {
                        NotificationService.notify(inv.getUserId(),
                                Notification.Type.OVERDUE_WARNING,
                                "Payment due in " + daysUntilDue + " day(s)",
                                "Hello,\n\nInvoice " + inv.getInvoiceNumber()
                                        + " (PKR " + inv.getRemainingBalance()
                                        + ") is due on " + inv.getDueDate() + ".\n\n"
                                        + "Please pay before the due date or your service will be cancelled "
                                        + "and your account may be blocked.",
                                "Invoice", inv.getInvoiceId(), true);
                        warned++;
                    }
                } else if (daysUntilDue < -3) {
                    // More than 3 days overdue → block user
                    if (inv.getUserId() != null) {
                        userDAO.blockUser(inv.getUserId(),
                                "Unpaid invoice " + inv.getInvoiceNumber()
                                        + " more than 3 days overdue");
                        NotificationService.notify(inv.getUserId(),
                                Notification.Type.ACCOUNT_BLOCKED,
                                "Account blocked",
                                "Your account has been blocked due to non-payment of invoice "
                                        + inv.getInvoiceNumber()
                                        + ". Please contact the vendor to restore access.",
                                "User", inv.getUserId(), true);
                        blocked++;
                    }
                }
            }

            dueDateStatusLabel.setText("✓ Due-date check complete. " + warned
                    + " warning(s) sent, " + blocked + " user(s) blocked.");
            dueDateStatusLabel.setStyle("-fx-text-fill: #16a34a;");
            com.paytrace.ui.utils.ToastNotification.show(
                    dueDateStatusLabel.getScene().getWindow(),
                    "✓ Due-date check: " + warned + " warned, " + blocked + " blocked",
                    com.paytrace.ui.utils.ToastNotification.ToastType.INFO);
        } catch (Exception e) {
            dueDateStatusLabel.setText("Failed: " + e.getMessage());
            dueDateStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    private void goBack() {
        try {SceneNavigator.loadIntoShell(SceneNavigator.DASHBOARD_HOME); }
        catch (Exception e) { e.printStackTrace(); }
    }
}